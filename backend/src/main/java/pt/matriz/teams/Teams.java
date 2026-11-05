package pt.matriz.teams;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.matriz.common.Reference;
import pt.matriz.teams.persistence.TeamQueries;

/** Interface pública do módulo Equipas (architecture.md, secções 5.1 e 8). */
@Service
public class Teams {

    private final TeamQueries queries;

    Teams(TeamQueries queries) {
        this.queries = queries;
    }

    /** Equipas a que a pessoa pertence, ordenadas por nome. Ignora as pertenças removidas (RN32). */
    public List<Reference> teamsOf(long userId) {
        return queries.activeTeamsOf(userId);
    }
}
