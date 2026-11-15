package pt.matriz.tasks.api;

import pt.matriz.tasks.TaskStatus;

/** Pedido de {@code PUT /api/tasks/{taskId}/status} (ChangeTaskStatusRequest em openapi.yaml). */
record ChangeTaskStatusRequest(TaskStatus status, Long version) {
}
