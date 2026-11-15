package pt.matriz.tasks.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import pt.matriz.tasks.TaskTotals;

/**
 * Consultas à tabela {@code tasks} (database.md, secção 3.5). As tarefas eliminadas
 * contam como inexistentes (RN32, DA29).
 */
@Repository
public class TaskQueries {

    private final JdbcClient jdbc;

    public TaskQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Tarefa atrasada: aberta e com prazo anterior à data atual (requirements.md, secção 2). */
    public Map<Long, TaskTotals> totalsByProject(Collection<Long> projectIds, LocalDate today) {
        Map<Long, TaskTotals> totals = new HashMap<>();
        if (projectIds.isEmpty()) {
            return totals;
        }
        jdbc.sql("""
                        SELECT project_id,
                               count(*) AS total,
                               count(*) FILTER (WHERE status = 'TODO') AS todo,
                               count(*) FILTER (WHERE status = 'IN_PROGRESS') AS in_progress,
                               count(*) FILTER (WHERE status = 'DONE') AS done,
                               count(*) FILTER (WHERE status IN ('TODO', 'IN_PROGRESS') AND due_date < :today) AS overdue
                        FROM tasks
                        WHERE deleted_at IS NULL AND project_id IN (:projectIds)
                        GROUP BY project_id
                        """)
                .param("projectIds", projectIds)
                .param("today", today)
                .query(rs -> {
                    totals.put(rs.getLong("project_id"), new TaskTotals(
                            rs.getLong("total"), rs.getLong("todo"), rs.getLong("in_progress"),
                            rs.getLong("done"), rs.getLong("overdue")));
                });
        return totals;
    }

    /**
     * Elimina as tarefas não eliminadas de um projeto, com a data e hora indicada (RN31, RN32).
     * A versão aumenta, como em todas as alterações automáticas (DA05). Os responsáveis e os
     * comentários ficam como estão.
     */
    public int markDeletedByProject(long projectId, OffsetDateTime deletedAt) {
        return jdbc.sql("""
                        UPDATE tasks SET deleted_at = :deletedAt, version = version + 1
                        WHERE project_id = :projectId AND deleted_at IS NULL
                        """)
                .param("deletedAt", deletedAt)
                .param("projectId", projectId)
                .update();
    }
}
