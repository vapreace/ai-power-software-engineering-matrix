package pt.matriz.tasks.persistence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.TaskStatus;
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

    /**
     * Tarefas não eliminadas de um projeto, com os filtros de US11 (todos ao mesmo tempo).
     * Ordem de DA33 e RN24: prazo com as tarefas sem prazo no fim, prioridade (Alta, Média,
     * Baixa), título sem maiúsculas nem acentos (DA14) e ordem de criação.
     *
     * @param statuses estados a mostrar; vazio mostra todos
     * @param priorities prioridades a mostrar; vazio mostra todas
     * @param overdueOnly só as tarefas atrasadas em {@code today}
     */
    public List<TaskRow> list(long projectId, Set<TaskStatus> statuses, Set<TaskPriority> priorities,
            boolean overdueOnly, LocalDate today) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, project_id, title, description, due_date, priority, status, version
                FROM tasks
                WHERE deleted_at IS NULL AND project_id = :projectId
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        if (!statuses.isEmpty()) {
            sql.append(" AND status IN (:statuses)");
            params.put("statuses", statuses.stream().map(Enum::name).toList());
        }
        if (!priorities.isEmpty()) {
            sql.append(" AND priority IN (:priorities)");
            params.put("priorities", priorities.stream().map(Enum::name).toList());
        }
        if (overdueOnly) {
            sql.append(" AND status IN ('TODO', 'IN_PROGRESS') AND due_date < :today");
            params.put("today", today);
        }
        sql.append("""
                 ORDER BY due_date ASC NULLS LAST,
                          CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END,
                          title COLLATE matriz_ci_ai,
                          id
                """);
        return jdbc.sql(sql.toString())
                .params(params)
                .query((rs, rowNum) -> new TaskRow(
                        rs.getLong("id"),
                        rs.getLong("project_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getObject("due_date", LocalDate.class),
                        TaskPriority.valueOf(rs.getString("priority")),
                        TaskStatus.valueOf(rs.getString("status")),
                        rs.getLong("version")))
                .list();
    }

    /** Identificadores dos responsáveis atuais de cada tarefa (associações não removidas, RN32). */
    public Map<Long, List<Long>> assigneeIds(Collection<Long> taskIds) {
        Map<Long, List<Long>> result = new HashMap<>();
        if (taskIds.isEmpty()) {
            return result;
        }
        jdbc.sql("""
                        SELECT task_id, user_id FROM task_assignees
                        WHERE task_id IN (:taskIds) AND removed_at IS NULL
                        """)
                .param("taskIds", taskIds)
                .query(rs -> {
                    result.computeIfAbsent(rs.getLong("task_id"), id -> new ArrayList<>()).add(rs.getLong("user_id"));
                });
        return result;
    }

    /** A pessoa é responsável pela tarefa (associação não removida). */
    public boolean isAssignee(long taskId, long userId) {
        Integer count = jdbc.sql("""
                        SELECT count(*) FROM task_assignees
                        WHERE task_id = :taskId AND user_id = :userId AND removed_at IS NULL
                        """)
                .param("taskId", taskId)
                .param("userId", userId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }
}
