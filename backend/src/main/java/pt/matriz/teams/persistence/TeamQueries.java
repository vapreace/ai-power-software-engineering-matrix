package pt.matriz.teams.persistence;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import pt.matriz.common.Reference;

/** Consultas às tabelas {@code teams} e {@code team_members} (database.md, secções 3.2 e 3.3). */
@Repository
public class TeamQueries {

    private final JdbcClient jdbc;

    public TeamQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Reference> activeTeamsOf(long userId) {
        return jdbc.sql("""
                        SELECT t.id, t.name
                        FROM teams t
                        JOIN team_members m ON m.team_id = t.id
                        WHERE m.user_id = :userId AND m.removed_at IS NULL
                        ORDER BY t.name COLLATE matriz_ci_ai
                        """)
                .param("userId", userId)
                .query((rs, rowNum) -> new Reference(rs.getLong("id"), rs.getString("name")))
                .list();
    }
}
