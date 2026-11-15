package pt.matriz.tasks;

import java.time.LocalDate;
import java.util.List;

import pt.matriz.common.Reference;

/**
 * Tarefa como a API a devolve (Task em openapi.yaml). {@code assignees} vem ordenado por nome;
 * vazio quer dizer "Sem responsável" (RN14). {@code overdue} usa a data atual da organização.
 */
public record TaskView(
        long id,
        long projectId,
        String title,
        String description,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        List<Reference> assignees,
        boolean overdue,
        long version) {
}
