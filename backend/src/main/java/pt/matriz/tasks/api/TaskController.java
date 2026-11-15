package pt.matriz.tasks.api;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.matriz.common.CurrentUserProvider;
import pt.matriz.common.Items;
import pt.matriz.tasks.CommentView;
import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.TaskStatus;
import pt.matriz.tasks.TaskView;
import pt.matriz.tasks.application.CommentService;
import pt.matriz.tasks.application.TaskService;

/**
 * Operações de tarefas de openapi.yaml (tags Tarefas, Responsáveis e Comentários): listar e
 * filtrar, criar, consultar, editar, mudar o estado e apagar (I3: US05, US07, US10, US11, US16);
 * responsáveis, filtro por responsável e comentários (I4: US06, US11, US19).
 * "As minhas tarefas" ({@code GET /me/tasks}) fica para o I7.
 */
@RestController
class TaskController {

    private final TaskService tasks;
    private final CommentService comments;
    private final CurrentUserProvider currentUser;

    TaskController(TaskService tasks, CommentService comments, CurrentUserProvider currentUser) {
        this.tasks = tasks;
        this.comments = comments;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    Items<TaskView> list(@PathVariable("projectId") long projectId,
            @RequestParam(name = "status", required = false) Set<TaskStatus> status,
            @RequestParam(name = "priority", required = false) Set<TaskPriority> priority,
            @RequestParam(name = "overdue", required = false, defaultValue = "false") boolean overdue,
            @RequestParam(name = "assigneeId", required = false) Long assigneeId,
            @RequestParam(name = "unassigned", required = false, defaultValue = "false") boolean unassigned) {
        return new Items<>(tasks.list(currentUser.get(), projectId, status, priority, overdue, assigneeId, unassigned));
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    ResponseEntity<TaskView> create(@PathVariable("projectId") long projectId, @RequestBody CreateTaskRequest body) {
        TaskView created = tasks.create(currentUser.get(), projectId, body.toCommand());
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    @GetMapping("/api/tasks/{taskId}")
    TaskView get(@PathVariable("taskId") long taskId) {
        return tasks.get(currentUser.get(), taskId);
    }

    @PatchMapping("/api/tasks/{taskId}")
    TaskView update(@PathVariable("taskId") long taskId, @RequestBody UpdateTaskRequest body) {
        return tasks.update(currentUser.get(), taskId, body.toCommand());
    }

    @PutMapping("/api/tasks/{taskId}/status")
    TaskView changeStatus(@PathVariable("taskId") long taskId, @RequestBody ChangeTaskStatusRequest body) {
        return tasks.changeStatus(currentUser.get(), taskId, body.status(), body.version());
    }

    @DeleteMapping("/api/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("taskId") long taskId) {
        tasks.delete(currentUser.get(), taskId);
    }

    @PutMapping("/api/tasks/{taskId}/assignees/{userId}")
    TaskView addAssignee(@PathVariable("taskId") long taskId, @PathVariable("userId") long userId) {
        return tasks.addAssignee(currentUser.get(), taskId, userId);
    }

    @DeleteMapping("/api/tasks/{taskId}/assignees/{userId}")
    TaskView removeAssignee(@PathVariable("taskId") long taskId, @PathVariable("userId") long userId) {
        return tasks.removeAssignee(currentUser.get(), taskId, userId);
    }

    @GetMapping("/api/tasks/{taskId}/comments")
    Items<CommentView> comments(@PathVariable("taskId") long taskId) {
        List<CommentView> found = comments.list(currentUser.get(), taskId);
        return new Items<>(found);
    }

    /** O autor e a data e hora vêm do backend: outros campos do pedido são ignorados (E43). */
    @PostMapping("/api/tasks/{taskId}/comments")
    ResponseEntity<CommentView> comment(@PathVariable("taskId") long taskId, @RequestBody CreateCommentRequest body) {
        CommentView created = comments.create(currentUser.get(), taskId, body.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
