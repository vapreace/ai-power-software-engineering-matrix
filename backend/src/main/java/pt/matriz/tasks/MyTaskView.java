package pt.matriz.tasks;

import java.time.LocalDate;
import java.util.List;

import pt.matriz.common.Reference;

/** Tarefa de "As minhas tarefas" (US08): os campos de {@link TaskView} mais o projeto (MyTask no contrato). */
public record MyTaskView(
        long id,
        long projectId,
        String title,
        String description,
        LocalDate dueDate,
        TaskPriority priority,
        TaskStatus status,
        List<Reference> assignees,
        boolean overdue,
        long version,
        Reference project) {

    public static MyTaskView of(TaskView task, String projectName) {
        return new MyTaskView(task.id(), task.projectId(), task.title(), task.description(), task.dueDate(),
                task.priority(), task.status(), task.assignees(), task.overdue(), task.version(),
                new Reference(task.projectId(), projectName));
    }
}
