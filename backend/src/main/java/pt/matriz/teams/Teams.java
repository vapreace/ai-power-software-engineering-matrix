package pt.matriz.teams;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.Accounts;
import pt.matriz.common.ApiException;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.ProblemCode;
import pt.matriz.common.Reference;
import pt.matriz.common.Role;
import pt.matriz.teams.persistence.TeamQueries;

/** Interface pública do módulo Equipas (architecture.md, secções 5.1 e 8). */
@Service
public class Teams {

    static final String TEAM_NOT_FOUND = "A equipa não existe.";
    static final String NO_ACCESS = "Não tem acesso a esta página.";

    private final TeamQueries queries;
    private final Accounts accounts;

    Teams(TeamQueries queries, Accounts accounts) {
        this.queries = queries;
        this.accounts = accounts;
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

    private static boolean seesAllTeams(CurrentUser user) {
        return user.role() == Role.ADMINISTRATOR || user.role() == Role.OBSERVER;
    }
}
