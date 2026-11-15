package pt.matriz.tasks.api;

import java.net.URI;
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
import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.TaskStatus;
import pt.matriz.tasks.TaskView;
import pt.matriz.tasks.application.TaskService;

/**
 * Operações de tarefas do I3 em openapi.yaml (tag Tarefas): listar e filtrar, criar, consultar,
 * editar, mudar o estado e apagar (US05, US07, US10, US11, US16). O filtro por responsável
 * ({@code assigneeId}, {@code unassigned}), os responsáveis e os comentários chegam no I4.
 */
@RestController
class TaskController {

    private final TaskService tasks;
    private final CurrentUserProvider currentUser;

    TaskController(TaskService tasks, CurrentUserProvider currentUser) {
        this.tasks = tasks;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    Items<TaskView> list(@PathVariable("projectId") long projectId,
            @RequestParam(name = "status", required = false) Set<TaskStatus> status,
            @RequestParam(name = "priority", required = false) Set<TaskPriority> priority,
            @RequestParam(name = "overdue", required = false, defaultValue = "false") boolean overdue) {
        return new Items<>(tasks.list(currentUser.get(), projectId, status, priority, overdue));
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
}
