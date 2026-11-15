package pt.matriz.tasks.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Pedido de {@code POST /api/tasks/{taskId}/comments} (CreateCommentRequest em openapi.yaml).
 * Campos como {@code authorId} ou {@code createdAt} são ignorados (RN30, E43).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record CreateCommentRequest(String content) {
}
