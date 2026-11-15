package pt.matriz.tasks.persistence;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consultas à tabela {@code task_comments} (database.md, secção 3.7). Os comentários não se
 * editam nem se apagam (RN30). Deixam de aparecer quando a tarefa é eliminada, porque só se
 * leem através de uma tarefa não eliminada.
 */
@Repository
public class CommentQueries {

    /** Comentário guardado, com o identificador do autor. */
    public record CommentRow(long id, long authorId, String content, OffsetDateTime createdAt) {
    }

    private final JdbcClient jdbc;

    public CommentQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Do mais antigo para o mais recente; o identificador desempata a mesma data e hora (RN30). */
    public List<CommentRow> list(long taskId) {
        return jdbc.sql("""
                        SELECT id, author_id, content, created_at FROM task_comments
                        WHERE task_id = :taskId
                        ORDER BY created_at, id
                        """)
                .param("taskId", taskId)
                .query((rs, rowNum) -> new CommentRow(rs.getLong("id"), rs.getLong("author_id"),
                        rs.getString("content"), rs.getObject("created_at", OffsetDateTime.class)))
                .list();
    }

    public long insert(long taskId, long authorId, String content, OffsetDateTime createdAt) {
        return jdbc.sql("""
                        INSERT INTO task_comments (task_id, author_id, content, created_at)
                        VALUES (:taskId, :authorId, :content, :createdAt)
                        RETURNING id
                        """)
                .param("taskId", taskId)
                .param("authorId", authorId)
                .param("content", content)
                .param("createdAt", createdAt)
                .query(Long.class)
                .single();
    }
}
