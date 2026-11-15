package pt.matriz.tasks.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.Accounts;
import pt.matriz.common.ApiException;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.OrganizationClock;
import pt.matriz.common.ProblemCode;
import pt.matriz.common.Reference;
import pt.matriz.common.Role;
import pt.matriz.common.Texts;
import pt.matriz.projects.ProjectAccess;
import pt.matriz.projects.Projects;
import pt.matriz.tasks.CommentView;
import pt.matriz.tasks.persistence.CommentQueries;
import pt.matriz.tasks.persistence.CommentQueries.CommentRow;
import pt.matriz.tasks.persistence.Task;
import pt.matriz.tasks.persistence.TaskRepository;

/**
 * Comentários de uma tarefa (US19, RN30). Quem vê o projeto lê os comentários, incluindo o
 * Observador. Comentam o Administrador e as pessoas que pertencem à equipa do projeto. O autor é
 * sempre quem está autenticado e a data e hora vêm do Relógio da organização (E43, RNF06).
 * As verificações seguem a ordem de DA17: 404, 403, 400, 409.
 */
@Service
public class CommentService {

    static final String TASK_NOT_FOUND = "A tarefa não existe.";
    static final String NO_ACCESS = "Não tem acesso a esta página.";
    static final String FORBIDDEN = "Não tem permissão para esta operação.";
    static final String CONTENT_INVALID = "O comentário é obrigatório e tem no máximo 2000 caracteres.";
    static final String PROJECT_CLOSED = "O projeto está concluído ou arquivado e não pode ser alterado.";
    static final int CONTENT_MAX = 2000;

    private final TaskRepository tasks;
    private final CommentQueries comments;
    private final Projects projects;
    private final Accounts accounts;
    private final OrganizationClock clock;

    CommentService(TaskRepository tasks, CommentQueries comments, Projects projects, Accounts accounts,
            OrganizationClock clock) {
        this.tasks = tasks;
        this.comments = comments;
        this.projects = projects;
        this.accounts = accounts;
        this.clock = clock;
    }

    /** Do mais antigo para o mais recente (RN30). */
    @Transactional(readOnly = true)
    public List<CommentView> list(CurrentUser user, long taskId) {
        Task task = findTask(taskId);
        if (!access(user, task).visible()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, NO_ACCESS);
        }
        return toViews(comments.list(task.getId()));
    }

    /** Comenta a tarefa. Comentar não muda a versão da tarefa (DA20). */
    @Transactional
    public CommentView create(CurrentUser user, long taskId, String content) {
        Task task = findTask(taskId);
        ProjectAccess access = access(user, task);
        boolean canComment = user.role() == Role.ADMINISTRATOR
                || ((user.role() == Role.MANAGER || user.role() == Role.MEMBER)
                        && user.teams().stream().anyMatch(team -> team.id() == access.teamId()));
        if (!canComment) {
            throw new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, FORBIDDEN);
        }
        String text = Texts.normalize(content);
        if (text == null || text.isEmpty() || Texts.length(text) > CONTENT_MAX) {
            throw ApiException.invalidField("content", CONTENT_INVALID);
        }
        if (!access.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, ProblemCode.PROJECT_CLOSED, PROJECT_CLOSED);
        }
        OffsetDateTime now = clock.now();
        long id = comments.insert(task.getId(), user.id(), text, now);
        return new CommentView(id, new Reference(user.id(), user.name()), text, now);
    }

    private Task findTask(long taskId) {
        return tasks.findActiveById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, TASK_NOT_FOUND));
    }

    private ProjectAccess access(CurrentUser user, Task task) {
        return projects.access(user, task.getProjectId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, TASK_NOT_FOUND));
    }

    private List<CommentView> toViews(List<CommentRow> rows) {
        Map<Long, String> names = accounts.findAllById(rows.stream().map(CommentRow::authorId).distinct().toList())
                .stream().collect(Collectors.toMap(AccountView::id, AccountView::name));
        return rows.stream()
                .map(row -> new CommentView(row.id(), new Reference(row.authorId(), names.get(row.authorId())),
                        row.content(), row.createdAt()))
                .toList();
    }
}
