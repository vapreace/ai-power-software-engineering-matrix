package pt.matriz.teams;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
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
import pt.matriz.teams.persistence.TeamQueries;

/** Interface pública do módulo Equipas (architecture.md, secções 5.1 e 8). */
@Service
public class Teams {

    static final String TEAM_NOT_FOUND = "A equipa não existe.";
    static final String NO_ACCESS = "Não tem acesso a esta página.";
    static final String FORBIDDEN = "Não tem permissão para esta operação.";
    static final String ACCOUNT_NOT_FOUND = "A conta não existe.";
    static final String NOT_A_MEMBER = "A pessoa não pertence à equipa.";
    static final String NAME_INVALID = "O nome é obrigatório e tem no máximo 100 caracteres.";
    static final String DUPLICATE_TEAM_NAME = "Já existe uma equipa com este nome.";
    static final String INVALID_TEAM_MEMBER = "Só pessoas com papel Gestor ou Membro e conta ativa pertencem a equipas.";
    static final String DUPLICATE_TEAM_MEMBER = "Esta pessoa já pertence à equipa.";

    static final int NAME_MAX = 100;

    private final TeamQueries queries;
    private final Accounts accounts;

    private final OrganizationClock clock;
    private final ApplicationEventPublisher events;

    Teams(TeamQueries queries, Accounts accounts, OrganizationClock clock, ApplicationEventPublisher events) {
        this.queries = queries;
        this.accounts = accounts;
        this.clock = clock;
        this.events = events;
    }

    /** Equipas a que a pessoa pertence, ordenadas por nome. Ignora as pertenças removidas (RN32). */
    @Transactional(readOnly = true)
    public List<Reference> teamsOf(long userId) {
        return queries.activeTeamsOf(userId);
    }

    /**
     * Equipas que a pessoa pode ver (requirements.md, secção 3.1): todas para o Administrador
     * e o Observador; as suas para o Gestor e o Membro. Ordenadas por nome.
     */
    @Transactional(readOnly = true)
    public List<Reference> visibleTeams(CurrentUser user) {
        return seesAllTeams(user) ? queries.allTeams() : queries.activeTeamsOf(user.id());
    }

    @Transactional(readOnly = true)
    public Optional<Reference> findTeam(long teamId) {
        return queries.findTeam(teamId);
    }

    /** Nome de cada equipa indicada, para mostrar nas listas. */
    @Transactional(readOnly = true)
    public Map<Long, String> teamNames(Collection<Long> teamIds) {
        return queries.teamsByIds(teamIds).stream().collect(Collectors.toMap(Reference::id, Reference::name));
    }

    /** A pessoa pertence à equipa (pertença não removida, RN32). */
    @Transactional(readOnly = true)
    public boolean belongsTo(long teamId, long userId) {
        return queries.isActiveMember(teamId, userId);
    }

    /** A equipa tem pelo menos um Gestor com conta ativa (RN27, E23, E38). */
    @Transactional(readOnly = true)
    public boolean hasActiveManager(long teamId) {
        return accounts.findAllById(queries.activeMemberIds(teamId)).stream()
                .anyMatch(account -> account.role() == Role.MANAGER && account.status() == AccountStatus.ACTIVE);
    }

    /**
     * Quem pertence à equipa, com o papel e o estado da conta, ordenado por nome sem distinguir
     * maiúsculas nem acentos. Administrador e Observador: qualquer equipa. Gestor e Membro: só as
     * equipas a que pertencem (requirements.md, secção 3.1). Equipa inexistente: 404.
     */
    @Transactional(readOnly = true)
    public List<TeamMember> members(CurrentUser user, long teamId) {
        if (queries.findTeam(teamId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, TEAM_NOT_FOUND);
        }
        if (!seesAllTeams(user) && user.teams().stream().noneMatch(team -> team.id() == teamId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, NO_ACCESS);
        }
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-PT"));
        collator.setStrength(Collator.PRIMARY);
        return accounts.findAllById(queries.activeMemberIds(teamId)).stream()
                .sorted(Comparator.comparing(AccountView::name, collator).thenComparingLong(AccountView::id))
                .map(account -> new TeamMember(account.id(), account.name(), account.role(), account.status()))
                .toList();
    }

    /** Criar equipa (US03). Nome obrigatório, até 100 caracteres, único sem distinguir maiúsculas. */
    @Transactional
    public Reference create(CurrentUser user, String name) {
        requireAdministrator(user);
        String validName = validName(name);
        if (queries.nameExists(validName, null)) {
            throw conflict(ProblemCode.DUPLICATE_TEAM_NAME, DUPLICATE_TEAM_NAME);
        }
        try {
            return new Reference(queries.insertTeam(validName), validName);
        } catch (DuplicateKeyException e) {
            throw conflict(ProblemCode.DUPLICATE_TEAM_NAME, DUPLICATE_TEAM_NAME);
        }
    }

    /** Mudar o nome (US03). Os projetos continuam a pertencer à equipa. */
    @Transactional
    public Reference rename(CurrentUser user, long teamId, String name) {
        findOrThrow(teamId);
        requireAdministrator(user);
        String validName = validName(name);
        if (queries.nameExists(validName, teamId)) {
            throw conflict(ProblemCode.DUPLICATE_TEAM_NAME, DUPLICATE_TEAM_NAME);
        }
        try {
            queries.renameTeam(teamId, validName);
        } catch (DuplicateKeyException e) {
            throw conflict(ProblemCode.DUPLICATE_TEAM_NAME, DUPLICATE_TEAM_NAME);
        }
        return new Reference(teamId, validName);
    }

    /** Adicionar uma pessoa (US03): só Gestor ou Membro com conta ativa (RN26). */
    @Transactional
    public void addMember(CurrentUser user, long teamId, long accountId) {
        findOrThrow(teamId);
        AccountView account = accounts.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, ACCOUNT_NOT_FOUND));
        requireAdministrator(user);
        boolean eligibleRole = account.role() == Role.MANAGER || account.role() == Role.MEMBER;
        if (!eligibleRole || account.status() != AccountStatus.ACTIVE) {
            throw conflict(ProblemCode.INVALID_TEAM_MEMBER, INVALID_TEAM_MEMBER);
        }
        if (queries.isActiveMember(teamId, accountId)) {
            throw conflict(ProblemCode.DUPLICATE_TEAM_MEMBER, DUPLICATE_TEAM_MEMBER);
        }
        try {
            queries.addMember(teamId, accountId);
        } catch (DuplicateKeyException e) {
            throw conflict(ProblemCode.DUPLICATE_TEAM_MEMBER, DUPLICATE_TEAM_MEMBER);
        }
    }

    /**
     * Remover uma pessoa (US03). As consequências (RN17, RN22) vêm do evento {@link TeamMemberRemoved}.
     * O papel verifica-se antes de saber se a pessoa pertence à equipa: assim, quem não é Administrador
     * não fica a saber quem pertence a outras equipas (F2 de quality-security-review.md).
     */
    @Transactional
    public void removeMember(CurrentUser user, long teamId, long accountId) {
        findOrThrow(teamId);
        requireAdministrator(user);
        if (!queries.isActiveMember(teamId, accountId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, NOT_A_MEMBER);
        }
        leave(teamId, accountId);
    }

    /** Quem passa a Observador ou Administrador sai de todas as equipas (RN23, RN26). */
    @Transactional
    public void removeFromAllTeams(long accountId) {
        for (Reference team : queries.activeTeamsOf(accountId)) {
            leave(team.id(), accountId);
        }
    }

    private void leave(long teamId, long accountId) {
        queries.removeMember(teamId, accountId, clock.now());
        events.publishEvent(new TeamMemberRemoved(teamId, accountId));
    }

    private void findOrThrow(long teamId) {
        if (queries.findTeam(teamId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, TEAM_NOT_FOUND);
        }
    }

    private static void requireAdministrator(CurrentUser user) {
        if (user.role() != Role.ADMINISTRATOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, FORBIDDEN);
        }
    }

    private static String validName(String value) {
        String name = Texts.normalize(value);
        if (name == null || name.isEmpty() || Texts.length(name) > NAME_MAX) {
            throw ApiException.invalidField("name", NAME_INVALID);
        }
        return name;
    }

    private static ApiException conflict(ProblemCode code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    private static boolean seesAllTeams(CurrentUser user) {
        return user.role() == Role.ADMINISTRATOR || user.role() == Role.OBSERVER;
    }
}
