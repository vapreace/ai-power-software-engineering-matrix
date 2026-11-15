package pt.matriz.tasks.application;

import java.text.Collator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
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
import pt.matriz.projects.ProjectAccess;
import pt.matriz.projects.Projects;
import pt.matriz.teams.Teams;
import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.TaskStatus;
import pt.matriz.tasks.TaskView;
import pt.matriz.tasks.persistence.Task;
import pt.matriz.tasks.persistence.TaskQueries;
import pt.matriz.tasks.persistence.TaskRepository;
import pt.matriz.tasks.persistence.TaskRow;

/**
 * Casos de uso das tarefas (US05, US07, US10, US11, US16), com as permissões de
 * requirements.md, secção 3.1. As verificações seguem a ordem de DA17: 404, 403, 400, 409.
 * <p>
 * Editar, criar, apagar e gerir os responsáveis: o Administrador e o gestor do projeto; num
 * projeto "Sem gestor", só o Administrador (RN22). Mudar o estado: também qualquer responsável
 * da tarefa (RN15). Os comentários estão em {@link CommentService}.
 */
@Service
public class TaskService {

    static final String PROJECT_NOT_FOUND = "O projeto não existe.";
    static final String TASK_NOT_FOUND = "A tarefa não existe.";
    static final String NO_ACCESS = "Não tem acesso a esta página.";
    static final String FORBIDDEN = "Não tem permissão para esta operação.";
    static final String TITLE_INVALID = "O título é obrigatório e tem no máximo 200 caracteres.";
    static final String DESCRIPTION_TOO_LONG = "A descrição tem no máximo 2000 caracteres.";
    static final String DUE_DATE_IN_PAST = "O prazo não pode ser anterior à data atual";
    static final String PRIORITY_REQUIRED = "A prioridade é obrigatória.";
    static final String STATUS_REQUIRED = "O estado é obrigatório.";
    static final String VERSION_REQUIRED = "A versão é obrigatória.";
    static final String NOTHING_TO_CHANGE = "Indique pelo menos um campo a alterar.";
    static final String FIELD_NOT_EDITABLE = "Este campo não pode ser alterado.";
    static final String PROJECT_NOT_ACTIVE = "Só projetos ativos aceitam novas tarefas.";
    static final String PROJECT_CLOSED = "O projeto está concluído ou arquivado e não pode ser alterado.";
    static final String VERSION_CONFLICT = "Esta tarefa foi alterada por outra pessoa. Abra-a de novo e repita a alteração.";
    static final String ACCOUNT_NOT_FOUND = "A conta não existe.";
    static final String NOT_AN_ASSIGNEE = "Esta pessoa não é responsável pela tarefa.";
    static final String INVALID_ASSIGNEE = "O responsável tem de ter conta ativa, pertencer à equipa do projeto e ter papel Gestor ou Membro.";
    static final String DUPLICATE_ASSIGNEE = "Esta pessoa já é responsável pela tarefa.";
    static final String ASSIGNEE_FILTERS = "Escolha um responsável ou \"Sem responsável\", não os dois.";

    static final int TITLE_MAX = 200;
    static final int DESCRIPTION_MAX = 2000;

    private final TaskRepository repository;
    private final TaskQueries queries;
    private final Projects projects;
    private final Accounts accounts;
    private final Teams teams;
    private final OrganizationClock clock;

    TaskService(TaskRepository repository, TaskQueries queries, Projects projects, Accounts accounts, Teams teams,
            OrganizationClock clock) {
        this.repository = repository;
        this.queries = queries;
        this.projects = projects;
        this.accounts = accounts;
        this.teams = teams;
        this.clock = clock;
    }

    /**
     * Tarefas do projeto com os filtros de US11 e a ordem de DA33. Só para quem vê o projeto.
     * O filtro por responsável mostra as tarefas em que essa pessoa é um dos responsáveis;
     * não se combina com "Sem responsável".
     */
    @Transactional(readOnly = true)
    public List<TaskView> list(CurrentUser user, long projectId, Set<TaskStatus> statuses,
            Set<TaskPriority> priorities, boolean overdueOnly, Long assigneeId, boolean unassigned) {
        ProjectAccess access = projectAccess(user, projectId);
        if (!access.visible()) {
            throw forbidden(NO_ACCESS);
        }
        if (assigneeId != null && unassigned) {
            throw ApiException.invalidField("unassigned", ASSIGNEE_FILTERS);
        }
        LocalDate today = clock.today();
        return toViews(queries.list(projectId, statuses == null ? Set.of() : statuses,
                priorities == null ? Set.of() : priorities, overdueOnly, assigneeId, unassigned, today), today);
    }

    /**
     * Cria uma tarefa "Por fazer" e sem responsáveis (US05, RN12). Prioridade "Média" se não for
     * indicada (RN11). O prazo não pode ser anterior à data atual (RN13). Só em projetos "Ativo" (RN08).
     */
    @Transactional
    public TaskView create(CurrentUser user, long projectId, CreateTaskCommand command) {
        ProjectAccess access = projectAccess(user, projectId);
        if (!access.manageable()) {
            throw forbidden(FORBIDDEN);
        }
        ValidationErrors errors = new ValidationErrors();
        String title = validTitle(command.title(), errors);
        String description = validDescription(command.description(), errors);
        LocalDate today = clock.today();
        if (command.dueDate() != null && command.dueDate().isBefore(today)) {
            errors.add("dueDate", DUE_DATE_IN_PAST);
        }
        errors.throwIfAny();
        if (!access.isActive()) {
            throw conflict(ProblemCode.PROJECT_NOT_ACTIVE, PROJECT_NOT_ACTIVE);
        }
        TaskPriority priority = command.priority() == null ? TaskPriority.MEDIUM : command.priority();
        Task saved = repository.saveAndFlush(Task.create(projectId, title, description, command.dueDate(), priority));
        return toView(TaskRow.of(saved), today);
    }

    /** Detalhe da tarefa, com os responsáveis. Tarefa apagada ou inexistente: 404 (RN32). */
    @Transactional(readOnly = true)
    public TaskView get(CurrentUser user, long taskId) {
        Task task = findTask(taskId);
        if (!taskProjectAccess(user, task).visible()) {
            throw forbidden(NO_ACCESS);
        }
        return toView(TaskRow.of(task), clock.today());
    }

    /**
     * Edita título, descrição, prazo e prioridade (US16). A validação do prazo só se aplica quando
     * o prazo muda (RN13, E18). Projetos fechados não mudam (RN09). A versão tem de ser a atual (DA05).
     */
    @Transactional
    public TaskView update(CurrentUser user, long taskId, UpdateTaskCommand command) {
        Task task = findTask(taskId);
        ProjectAccess access = taskProjectAccess(user, task);
        if (!access.manageable()) {
            throw forbidden(FORBIDDEN);
        }

        ValidationErrors errors = new ValidationErrors();
        command.unknownFields().forEach(field -> errors.add(field, FIELD_NOT_EDITABLE));
        if (command.version() == null) {
            errors.add("version", VERSION_REQUIRED);
        }
        String title = command.title().present() ? validTitle(command.title().value(), errors) : task.getTitle();
        String description = command.description().present()
                ? validDescription(command.description().value(), errors)
                : task.getDescription();
        LocalDate today = clock.today();
        LocalDate dueDate = command.dueDate().orElse(task.getDueDate());
        boolean dueDateChanged = command.dueDate().present() && dueDate != null && !dueDate.equals(task.getDueDate());
        if (dueDateChanged && dueDate.isBefore(today)) {
            errors.add("dueDate", DUE_DATE_IN_PAST);
        }
        TaskPriority priority = command.priority().orElse(task.getPriority());
        if (command.priority().present() && priority == null) {
            errors.add("priority", PRIORITY_REQUIRED);
        }
        errors.throwIfAny();
        if (!command.changesSomething()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, NOTHING_TO_CHANGE);
        }

        requireOpenProject(access);
        requireVersion(task, command.version());
        task.edit(title, description, dueDate, priority);
        flush(task);
        return toView(TaskRow.of(task), today);
    }

    /**
     * Muda o estado (US07). Qualquer mudança entre os três estados é permitida (RN12). Pode o
     * Administrador, o gestor do projeto e qualquer responsável da tarefa, qualquer que seja o papel
     * (RN15), mesmo num projeto "Sem gestor" (RN22). O Observador nunca pode.
     */
    @Transactional
    public TaskView changeStatus(CurrentUser user, long taskId, TaskStatus status, Long version) {
        Task task = findTask(taskId);
        ProjectAccess access = taskProjectAccess(user, task);
        boolean assignee = access.visible() && queries.isAssignee(task.getId(), user.id());
        if (!access.manageable() && !assignee) {
            throw forbidden(FORBIDDEN);
        }
        ValidationErrors errors = new ValidationErrors();
        if (status == null) {
            errors.add("status", STATUS_REQUIRED);
        }
        if (version == null) {
            errors.add("version", VERSION_REQUIRED);
        }
        errors.throwIfAny();
        requireOpenProject(access);
        requireVersion(task, version);
        task.changeStatus(status);
        flush(task);
        return toView(TaskRow.of(task), clock.today());
    }

    /**
     * Apaga a tarefa (US16, RN19): eliminação lógica, definitiva para os utilizadores (RN32).
     * A confirmação é feita no frontend. Não leva versão (DA20).
     */
    @Transactional
    public void delete(CurrentUser user, long taskId) {
        Task task = findTask(taskId);
        ProjectAccess access = taskProjectAccess(user, task);
        if (!access.manageable()) {
            throw forbidden(FORBIDDEN);
        }
        requireOpenProject(access);
        task.markDeleted(clock.now());
        repository.saveAndFlush(task);
    }

    /**
     * Associa um responsável (US06). Tem conta ativa, pertence à equipa do projeto e tem papel
     * Gestor ou Membro (RN14). A mesma pessoa não fica duas vezes (E40). A versão aumenta (DA20).
     */
    @Transactional
    public TaskView addAssignee(CurrentUser user, long taskId, long accountId) {
        Task task = findTask(taskId);
        AccountView account = accounts.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, ACCOUNT_NOT_FOUND));
        ProjectAccess access = taskProjectAccess(user, task);
        if (!access.manageable()) {
            throw forbidden(FORBIDDEN);
        }
        requireOpenProject(access);
        boolean eligible = account.status() == AccountStatus.ACTIVE
                && (account.role() == Role.MANAGER || account.role() == Role.MEMBER)
                && teams.belongsTo(access.teamId(), account.id());
        if (!eligible) {
            throw conflict(ProblemCode.INVALID_ASSIGNEE, INVALID_ASSIGNEE);
        }
        if (queries.isAssignee(task.getId(), account.id())) {
            throw conflict(ProblemCode.DUPLICATE_ASSIGNEE, DUPLICATE_ASSIGNEE);
        }
        try {
            queries.addAssignee(task.getId(), account.id());
        } catch (DataIntegrityViolationException e) {
            throw conflict(ProblemCode.DUPLICATE_ASSIGNEE, DUPLICATE_ASSIGNEE);
        }
        queries.incrementVersion(task.getId());
        return toView(queries.findRow(task.getId()), clock.today());
    }

    /**
     * Remove um responsável (US06): a associação fica marcada como removida (RN32). Os outros
     * responsáveis mantêm-se; sem nenhum, a tarefa fica "Sem responsável" (E41). A versão aumenta (DA20).
     */
    @Transactional
    public TaskView removeAssignee(CurrentUser user, long taskId, long accountId) {
        Task task = findTask(taskId);
        if (!queries.isAssignee(task.getId(), accountId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, NOT_AN_ASSIGNEE);
        }
        ProjectAccess access = taskProjectAccess(user, task);
        if (!access.manageable()) {
            throw forbidden(FORBIDDEN);
        }
        requireOpenProject(access);
        queries.removeAssignee(task.getId(), accountId, clock.now());
        queries.incrementVersion(task.getId());
        return toView(queries.findRow(task.getId()), clock.today());
    }

    private ProjectAccess projectAccess(CurrentUser user, long projectId) {
        return projects.access(user, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, PROJECT_NOT_FOUND));
    }

    /** O projeto de uma tarefa não eliminada existe sempre: eliminar o projeto elimina as tarefas (RN31). */
    private ProjectAccess taskProjectAccess(CurrentUser user, Task task) {
        return projects.access(user, task.getProjectId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, TASK_NOT_FOUND));
    }

    private Task findTask(long taskId) {
        return repository.findActiveById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, TASK_NOT_FOUND));
    }

    private static void requireOpenProject(ProjectAccess access) {
        if (!access.isActive()) {
            throw conflict(ProblemCode.PROJECT_CLOSED, PROJECT_CLOSED);
        }
    }

    private static void requireVersion(Task task, Long version) {
        if (version != task.getVersion()) {
            throw conflict(ProblemCode.VERSION_CONFLICT, VERSION_CONFLICT);
        }
    }

    private void flush(Task task) {
        try {
            repository.saveAndFlush(task);
        } catch (OptimisticLockingFailureException e) {
            throw conflict(ProblemCode.VERSION_CONFLICT, VERSION_CONFLICT);
        }
    }

    private static String validTitle(String value, ValidationErrors errors) {
        String title = Texts.normalize(value);
        if (title == null || title.isEmpty() || Texts.length(title) > TITLE_MAX) {
            errors.add("title", TITLE_INVALID);
        }
        return title;
    }

    private static String validDescription(String value, ValidationErrors errors) {
        String description = Texts.normalizeOptional(value);
        if (Texts.length(description) > DESCRIPTION_MAX) {
            errors.add("description", DESCRIPTION_TOO_LONG);
        }
        return description;
    }

    private List<TaskView> toViews(List<TaskRow> rows, LocalDate today) {
        Map<Long, List<Long>> assigneeIds = queries.assigneeIds(rows.stream().map(TaskRow::id).toList());
        Map<Long, String> names = accounts.findAllById(assigneeIds.values().stream()
                        .flatMap(List::stream).distinct().toList())
                .stream().collect(Collectors.toMap(AccountView::id, AccountView::name));
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-PT"));
        collator.setStrength(Collator.PRIMARY);
        Comparator<Reference> byName = Comparator.comparing(Reference::name, collator)
                .thenComparingLong(Reference::id);
        return rows.stream().map(row -> {
            List<Reference> assignees = assigneeIds.getOrDefault(row.id(), List.of()).stream()
                    .map(id -> new Reference(id, names.get(id)))
                    .sorted(byName)
                    .toList();
            return new TaskView(row.id(), row.projectId(), row.title(), row.description(), row.dueDate(),
                    row.priority(), row.status(), assignees, isOverdue(row, today), row.version());
        }).toList();
    }

    private TaskView toView(TaskRow row, LocalDate today) {
        return toViews(List.of(row), today).getFirst();
    }

    /** Aberta e com prazo anterior à data atual; sem prazo, nunca (requirements.md, secção 2; E2, E3, E4). */
    private static boolean isOverdue(TaskRow row, LocalDate today) {
        return row.status() != TaskStatus.DONE && row.dueDate() != null && row.dueDate().isBefore(today);
    }

    private static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, message);
    }

    private static ApiException conflict(ProblemCode code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
