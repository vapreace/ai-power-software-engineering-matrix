package pt.matriz.projects;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.Accounts;
import pt.matriz.common.ApiException;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.OrganizationClock;
import pt.matriz.common.ProblemCode;
import pt.matriz.common.Reference;
import pt.matriz.common.Role;
import pt.matriz.common.Texts;
import pt.matriz.common.ValidationErrors;
import pt.matriz.projects.persistence.Project;
import pt.matriz.projects.persistence.ProjectQueries;
import pt.matriz.projects.persistence.ProjectRepository;
import pt.matriz.projects.persistence.ProjectRow;
import pt.matriz.teams.Teams;

/**
 * Interface pública do módulo Projetos (architecture.md, secções 5.1 e 8): criar, consultar,
 * editar, eliminar e pesquisar projetos, com as permissões de requirements.md, secção 3.1.
 * <p>
 * As verificações seguem a ordem de DA17: 404, 403, 400, 409. A mudança de estado (US13) e
 * do gestor (US17) ficam para o I7.
 */
@Service
public class Projects {

    static final String PROJECT_NOT_FOUND = "O projeto não existe.";
    static final String TEAM_NOT_FOUND = "A equipa não existe.";
    static final String NO_ACCESS = "Não tem acesso a esta página.";
    static final String FORBIDDEN = "Não tem permissão para esta operação.";
    static final String NAME_INVALID = "O nome é obrigatório e tem no máximo 100 caracteres.";
    static final String DESCRIPTION_TOO_LONG = "A descrição tem no máximo 1000 caracteres.";
    static final String TEAM_REQUIRED = "A equipa é obrigatória.";
    static final String START_DATE_REQUIRED = "A data de início é obrigatória.";
    static final String DATES_INCOHERENT = "A data de fim prevista não pode ser anterior à data de início.";
    static final String MANAGER_REQUIRED = "O gestor é obrigatório.";
    static final String VERSION_REQUIRED = "A versão é obrigatória.";
    static final String NOTHING_TO_CHANGE = "Indique pelo menos um campo a alterar.";
    static final String FIELD_NOT_EDITABLE = "Este campo não pode ser alterado.";
    static final String QUERY_TOO_LONG = "A pesquisa tem no máximo 100 caracteres.";
    static final String TEAM_WITHOUT_MANAGER = "A equipa não tem nenhum Gestor";
    static final String INVALID_MANAGER = "O gestor tem de ter conta ativa, pertencer à equipa do projeto e ter papel Gestor.";
    static final String PROJECT_CLOSED = "O projeto está concluído ou arquivado e não pode ser alterado.";
    static final String VERSION_CONFLICT = "Este projeto foi alterado por outra pessoa. Abra-o de novo e repita a alteração.";

    static final int NAME_MAX = 100;
    static final int DESCRIPTION_MAX = 1000;
    static final int QUERY_MAX = 100;

    private final ProjectRepository repository;
    private final ProjectQueries queries;
    private final Teams teams;
    private final Accounts accounts;
    private final OrganizationClock clock;
    private final ApplicationEventPublisher events;

    Projects(ProjectRepository repository, ProjectQueries queries, Teams teams, Accounts accounts,
            OrganizationClock clock, ApplicationEventPublisher events) {
        this.repository = repository;
        this.queries = queries;
        this.teams = teams;
        this.accounts = accounts;
        this.clock = clock;
        this.events = events;
    }

    /**
     * Projetos visíveis (requirements.md, secção 2), filtrados por estado e pesquisados pelo nome.
     * Sem estados indicados, mostra "Ativo" e "Concluído" (RN25). A pesquisa segue RN34 e DA32.
     */
    @Transactional(readOnly = true)
    public List<ProjectView> search(CurrentUser user, Set<ProjectStatus> statuses, String query) {
        String text = Texts.normalize(query);
        if (Texts.length(text) > QUERY_MAX) {
            throw ApiException.invalidField("q", QUERY_TOO_LONG);
        }
        // Um parâmetro vazio (?status=) chega como null: ignora-se, em vez de dar erro 500.
        Set<ProjectStatus> chosen = statuses == null ? Set.of()
                : statuses.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
        Set<ProjectStatus> effective = chosen.isEmpty()
                ? EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.COMPLETED)
                : chosen;
        List<Long> teamIds = seesAllProjects(user) ? null : user.teams().stream().map(Reference::id).toList();
        if (teamIds != null && teamIds.isEmpty()) {
            return List.of(); // Gestor ou Membro sem equipa (RN26, E26).
        }
        return toViews(queries.search(effective, teamIds, text == null || text.isEmpty() ? null : text));
    }

    /** Projeto visível. Eliminado ou inexistente: 404 (RN32). Não visível: 403 (SEG02). */
    @Transactional(readOnly = true)
    public ProjectView get(CurrentUser user, long projectId) {
        Project project = findProject(projectId);
        if (!isVisible(user, project.getTeamId())) {
            throw forbidden(NO_ACCESS);
        }
        return toView(project.toRow());
    }

    /**
     * Acesso de uma pessoa a um projeto, sem lançar erros, para os outros módulos aplicarem
     * a ordem de DA17. Vazio quando o projeto não existe ou foi eliminado (RN32).
     */
    @Transactional(readOnly = true)
    public Optional<ProjectAccess> access(CurrentUser user, long projectId) {
        return repository.findActiveById(projectId).map(project -> new ProjectAccess(project.getId(),
                project.getTeamId(), project.getStatus(), isVisible(user, project.getTeamId()), canManage(user, project)));
    }

    /**
     * Cria um projeto "Ativo" (US04, RN07). O Gestor cria nas suas equipas e fica gestor (RN05, RN27).
     * O Administrador cria em qualquer equipa e indica o gestor (RN21, RN27).
     */
    @Transactional
    public ProjectView create(CurrentUser user, CreateProjectCommand command) {
        if (command.teamId() != null && teams.findTeam(command.teamId()).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, TEAM_NOT_FOUND);
        }
        boolean administrator = user.role() == Role.ADMINISTRATOR;
        boolean manager = user.role() == Role.MANAGER
                && (command.teamId() == null || belongsTo(user, command.teamId()));
        if (!administrator && !manager) {
            throw forbidden(FORBIDDEN);
        }

        ValidationErrors errors = new ValidationErrors();
        if (command.teamId() == null) {
            errors.add("teamId", TEAM_REQUIRED);
        }
        String name = validName(command.name(), errors);
        String description = validDescription(command.description(), errors);
        if (command.startDate() == null) {
            errors.add("startDate", START_DATE_REQUIRED);
        }
        checkDates(command.startDate(), command.expectedEndDate(), errors);
        if (administrator && command.managerId() == null) {
            errors.add("managerId", MANAGER_REQUIRED);
        }
        errors.throwIfAny();

        long teamId = command.teamId();
        if (!teams.hasActiveManager(teamId)) {
            throw conflict(ProblemCode.TEAM_WITHOUT_MANAGER, TEAM_WITHOUT_MANAGER);
        }
        // O campo managerId é ignorado quando quem cria é Gestor (RN27).
        long managerId = administrator ? command.managerId() : user.id();
        if (administrator && !isValidManager(managerId, teamId)) {
            throw conflict(ProblemCode.INVALID_MANAGER, INVALID_MANAGER);
        }
        Project saved = repository.saveAndFlush(Project.create(teamId, managerId, name, description,
                command.startDate(), command.expectedEndDate()));
        return toView(saved.toRow());
    }

    /**
     * Edita nome, descrição e datas (US17). Só o Administrador e o gestor do projeto; num projeto
     * "Sem gestor", só o Administrador (RN05, RN22). Projetos fechados não mudam (RN09). A versão
     * tem de ser a atual (DA05).
     */
    @Transactional
    public ProjectView update(CurrentUser user, long projectId, UpdateProjectCommand command) {
        Project project = findProject(projectId);
        requireCanManage(user, project);

        ValidationErrors errors = new ValidationErrors();
        command.unknownFields().forEach(field -> errors.add(field, FIELD_NOT_EDITABLE));
        if (command.version() == null) {
            errors.add("version", VERSION_REQUIRED);
        }
        String name = command.name().present() ? validName(command.name().value(), errors) : project.getName();
        String description = command.description().present()
                ? validDescription(command.description().value(), errors)
                : project.getDescription();
        LocalDate startDate = command.startDate().orElse(project.getStartDate());
        if (command.startDate().present() && startDate == null) {
            errors.add("startDate", START_DATE_REQUIRED);
        }
        LocalDate expectedEndDate = command.expectedEndDate().orElse(project.getExpectedEndDate());
        checkDates(startDate, expectedEndDate, errors);
        errors.throwIfAny();
        if (!command.changesSomething()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, NOTHING_TO_CHANGE);
        }

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw conflict(ProblemCode.PROJECT_CLOSED, PROJECT_CLOSED);
        }
        if (command.version() != project.getVersion()) {
            throw conflict(ProblemCode.VERSION_CONFLICT, VERSION_CONFLICT);
        }
        project.edit(name, description, startDate, expectedEndDate);
        try {
            repository.saveAndFlush(project);
        } catch (OptimisticLockingFailureException e) {
            throw conflict(ProblemCode.VERSION_CONFLICT, VERSION_CONFLICT);
        }
        return toView(project.toRow());
    }

    /**
     * Elimina o projeto (US20, RN31): eliminação lógica, em qualquer estado (exceção a RN09).
     * Publica "Projeto eliminado" para as tarefas ficarem eliminadas na mesma transação (RN32).
     */
    @Transactional
    public void delete(CurrentUser user, long projectId) {
        Project project = findProject(projectId);
        requireCanManage(user, project);
        OffsetDateTime now = clock.now();
        project.markDeleted(now);
        repository.saveAndFlush(project);
        events.publishEvent(new ProjectDeleted(project.getId(), now));
    }

    private Project findProject(long projectId) {
        return repository.findActiveById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, PROJECT_NOT_FOUND));
    }

    private void requireCanManage(CurrentUser user, Project project) {
        if (!canManage(user, project)) {
            throw forbidden(FORBIDDEN);
        }
    }

    /** Administrador, ou o gestor do projeto enquanto cumpre RN21 (papel Gestor e pertence à equipa). */
    private static boolean canManage(CurrentUser user, Project project) {
        if (user.role() == Role.ADMINISTRATOR) {
            return true;
        }
        return user.role() == Role.MANAGER
                && project.getManagerId() != null
                && project.getManagerId() == user.id()
                && belongsTo(user, project.getTeamId());
    }

    private boolean isValidManager(long accountId, long teamId) {
        return accounts.findById(accountId)
                .filter(account -> account.role() == Role.MANAGER && account.status() == AccountStatus.ACTIVE)
                .isPresent()
                && teams.belongsTo(teamId, accountId);
    }

    private static boolean seesAllProjects(CurrentUser user) {
        return user.role() == Role.ADMINISTRATOR || user.role() == Role.OBSERVER;
    }

    private static boolean isVisible(CurrentUser user, long teamId) {
        return seesAllProjects(user) || belongsTo(user, teamId);
    }

    private static boolean belongsTo(CurrentUser user, long teamId) {
        return user.teams().stream().anyMatch(team -> team.id() == teamId);
    }

    private static String validName(String value, ValidationErrors errors) {
        String name = Texts.normalize(value);
        if (name == null || name.isEmpty() || Texts.length(name) > NAME_MAX) {
            errors.add("name", NAME_INVALID);
        }
        return name;
    }

    private static String validDescription(String value, ValidationErrors errors) {
        String description = Texts.normalizeOptional(value);
        if (Texts.length(description) > DESCRIPTION_MAX) {
            errors.add("description", DESCRIPTION_TOO_LONG);
        }
        return description;
    }

    private static void checkDates(LocalDate startDate, LocalDate expectedEndDate, ValidationErrors errors) {
        if (startDate != null && expectedEndDate != null && expectedEndDate.isBefore(startDate)) {
            errors.add("expectedEndDate", DATES_INCOHERENT);
        }
    }

    private List<ProjectView> toViews(List<ProjectRow> rows) {
        Map<Long, String> teamNames = teams.teamNames(rows.stream().map(ProjectRow::teamId).distinct().toList());
        Map<Long, String> managerNames = accounts.findAllById(rows.stream()
                        .map(ProjectRow::managerId).filter(id -> id != null).distinct().toList())
                .stream().collect(Collectors.toMap(AccountView::id, AccountView::name));
        return rows.stream().map(row -> toView(row, teamNames::get, managerNames::get)).toList();
    }

    private ProjectView toView(ProjectRow row) {
        return toViews(List.of(row)).getFirst();
    }

    private static ProjectView toView(ProjectRow row, Function<Long, String> teamName, Function<Long, String> managerName) {
        Reference manager = row.managerId() == null ? null : new Reference(row.managerId(), managerName.apply(row.managerId()));
        return new ProjectView(row.id(), row.name(), row.description(), new Reference(row.teamId(), teamName.apply(row.teamId())),
                manager, row.status(), row.startDate(), row.expectedEndDate(), row.version());
    }

    private static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, message);
    }

    private static ApiException conflict(ProblemCode code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
