package pt.matriz.projects.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import pt.matriz.projects.ProjectStatus;

/**
 * Lista e pesquisa de projetos (RF09, RF40). Só devolve projetos não eliminados (RN32).
 * A pesquisa compara em minúsculas e sem acentos, com {@code unaccent} (DA32), e trata
 * {@code %} e {@code _} como texto. A ordem é por nome, sem maiúsculas nem acentos (DA27, DA14).
 */
@Repository
public class ProjectQueries {

    private final JdbcClient jdbc;

    public ProjectQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param teamIds equipas visíveis, ou {@code null} para todas (Administrador e Observador)
     * @param query texto já normalizado, ou {@code null} sem pesquisa
     */
    public List<ProjectRow> search(Set<ProjectStatus> statuses, Collection<Long> teamIds, String query) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, team_id, manager_id, name, description, start_date, expected_end_date, status, version
                FROM projects
                WHERE deleted_at IS NULL AND status IN (:statuses)
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("statuses", statuses.stream().map(Enum::name).toList());
        if (teamIds != null) {
            sql.append(" AND team_id IN (:teamIds)");
            params.put("teamIds", teamIds);
        }
        if (query != null) {
            sql.append(" AND unaccent(lower(name)) LIKE '%' || unaccent(lower(:query)) || '%' ESCAPE '\\'");
            params.put("query", escapeLike(query));
        }
        sql.append(" ORDER BY name COLLATE matriz_ci_ai, id");
        return jdbc.sql(sql.toString())
                .params(params)
                .query((rs, rowNum) -> new ProjectRow(
                        rs.getLong("id"),
                        rs.getLong("team_id"),
                        rs.getObject("manager_id", Long.class),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getObject("start_date", java.time.LocalDate.class),
                        rs.getObject("expected_end_date", java.time.LocalDate.class),
                        ProjectStatus.valueOf(rs.getString("status")),
                        rs.getLong("version")))
                .list();
    }

    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
    /**
     * O projeto fica "Sem gestor" (RN22). Aplica-se também a projetos fechados (exceção de RN09).
     * Com {@code teamId}, só os projetos dessa equipa.
     */
    public int clearManager(long managerId, Long teamId) {
        String sql = """
                UPDATE projects SET manager_id = NULL, version = version + 1
                WHERE manager_id = :managerId AND deleted_at IS NULL
                """ + (teamId == null ? "" : " AND team_id = :teamId");
        var statement = jdbc.sql(sql).param("managerId", managerId);
        if (teamId != null) {
            statement = statement.param("teamId", teamId);
        }
        return statement.update();
    }

    /** Projetos não eliminados de uma equipa, em qualquer estado. */
    public List<Long> idsOfTeam(long teamId) {
        return jdbc.sql("SELECT id FROM projects WHERE team_id = :teamId AND deleted_at IS NULL")
                .param("teamId", teamId)
                .query(Long.class)
                .list();
    }

    /** Nomes dos projetos "Ativo" não eliminados, entre os indicados. */
    public Map<Long, String> activeNames(Collection<Long> projectIds) {
        Map<Long, String> names = new HashMap<>();
        if (projectIds.isEmpty()) {
            return names;
        }
        jdbc.sql("""
                        SELECT id, name FROM projects
                        WHERE id IN (:projectIds) AND status = 'ACTIVE' AND deleted_at IS NULL
                        """)
                .param("projectIds", projectIds)
                .query(rs -> {
                    names.put(rs.getLong("id"), rs.getString("name"));
                });
        return names;
    }
}
