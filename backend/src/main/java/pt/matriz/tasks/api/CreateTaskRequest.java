package pt.matriz.tasks.api;

import java.time.LocalDate;

import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.application.CreateTaskCommand;

/**
 * Pedido de {@code POST /api/projects/{projectId}/tasks} (CreateTaskRequest em openapi.yaml).
 * A validação é feita depois das permissões, para seguir a ordem de DA17.
 */
record CreateTaskRequest(String title, String description, LocalDate dueDate, TaskPriority priority) {

    CreateTaskCommand toCommand() {
        return new CreateTaskCommand(title, description, dueDate, priority);
    }
}
