package pt.matriz.projects;

import java.time.OffsetDateTime;

/**
 * Evento "Projeto eliminado" (architecture.md, secção 7). É publicado na mesma transação.
 * O módulo Tarefas reage e elimina as tarefas do projeto com a mesma data e hora (RN31, RN32).
 */
public record ProjectDeleted(long projectId, OffsetDateTime deletedAt) {
}
