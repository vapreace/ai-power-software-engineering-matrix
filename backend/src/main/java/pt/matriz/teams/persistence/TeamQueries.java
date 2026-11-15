package pt.matriz.teams.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import pt.matriz.common.Reference;

/**
 * Consultas às tabelas {@code teams} e {@code team_members} (database.md, secções 3.2 e 3.3).
 * As pertenças removidas ({@code removed_at} preenchido) contam como inexistentes (RN32, DA29).
 */
@Repository
public class TeamQueries {

    private static final String ACTIVE_MEMBERSHIP = "m.removed_at IS NULL";

    private final JdbcClient jdbc;

    public TeamQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Reference> activeTeamsOf(long userId) {
        return jdbc.sql("""
                        SELECT t.id, t.name
                        FROM teams t
                        JOIN team_members m ON m.team_id = t.id
                        WHERE m.user_id = :userId AND %s
                        ORDER BY t.name COLLATE matriz_ci_ai
                        """.formatted(ACTIVE_MEMBERSHIP))
                .param("userId", userId)
                .query((rs, rowNum) -> new Reference(rs.getLong("id"), rs.getString("name")))
                .list();
    }

    public List<Reference> allTeams() {
        return jdbc.sql("SELECT id, name FROM teams ORDER BY name COLLATE matriz_ci_ai")
                .query((rs, rowNum) -> new Reference(rs.getLong("id"), rs.getString("name")))
                .list();
    }

    public Optional<Reference> findTeam(long teamId) {
        return jdbc.sql("SELECT id, name FROM teams WHERE id = :teamId")
                .param("teamId", teamId)
                .query((rs, rowNum) -> new Reference(rs.getLong("id"), rs.getString("name")))
                .optional();
    }

    public List<Reference> teamsByIds(Collection<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT id, name FROM teams WHERE id IN (:teamIds)")
                .param("teamIds", teamIds)
                .query((rs, rowNum) -> new Reference(rs.getLong("id"), rs.getString("name")))
                .list();
    }

    /** Identificadores das contas que pertencem à equipa. */
    public List<Long> activeMemberIds(long teamId) {
        return jdbc.sql("SELECT m.user_id FROM team_members m WHERE m.team_id = :teamId AND " + ACTIVE_MEMBERSHIP)
                .param("teamId", teamId)
                .query(Long.class)
                .list();
    }

    public boolean isActiveMember(long teamId, long userId) {
        Integer count = jdbc.sql("""
                        SELECT count(*) FROM team_members m
                        WHERE m.team_id = :teamId AND m.user_id = :userId AND %s
                        """.formatted(ACTIVE_MEMBERSHIP))
                .param("teamId", teamId)
                .param("userId", userId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }
}
