package pt.matriz.tasks.persistence;

import java.time.LocalDate;

import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.TaskStatus;

/** Linha da tabela {@code tasks} sem a data de eliminação, para as leituras. */
public record TaskRow(
        long id,
        long projectId,
        String title,
        String description,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        long version) {

    public static TaskRow of(Task task) {
        return new TaskRow(task.getId(), task.getProjectId(), task.getTitle(), task.getDescription(),
                task.getDueDate(), task.getPriority(), task.getStatus(), task.getVersion());
    }
}
