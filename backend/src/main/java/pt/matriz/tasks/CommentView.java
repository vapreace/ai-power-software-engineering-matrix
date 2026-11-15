package pt.matriz.tasks;

import java.time.OffsetDateTime;

import pt.matriz.common.Reference;

/** Comentário de uma tarefa (Comment em openapi.yaml). O texto é mostrado sempre como texto (SEG06). */
public record CommentView(long id, Reference author, String content, OffsetDateTime createdAt) {
}
