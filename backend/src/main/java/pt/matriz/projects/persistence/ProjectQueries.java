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
}
