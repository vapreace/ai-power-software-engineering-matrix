package pt.matriz.tasks.application;

import java.time.LocalDate;
import java.util.Set;

import pt.matriz.common.Patch;
import pt.matriz.tasks.TaskPriority;

/**
 * Dados para editar uma tarefa (UpdateTaskRequest em openapi.yaml, US16). Só os campos enviados
 * mudam. {@code unknownFields} são campos que não se editam aqui, como o estado (DA19) ou o projeto (RN20).
 */
public record UpdateTaskCommand(
        Long version,
        Patch<String> title,
        Patch<String> description,
        Patch<LocalDate> dueDate,
        Patch<TaskPriority> priority,
        Set<String> unknownFields) {

    boolean changesSomething() {
        return title.present() || description.present() || dueDate.present() || priority.present()
                || !unknownFields.isEmpty();
    }
}
