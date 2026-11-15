package pt.matriz.tasks.api;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import pt.matriz.common.Patch;
import pt.matriz.tasks.TaskPriority;
import pt.matriz.tasks.application.UpdateTaskCommand;

/**
 * Pedido de {@code PATCH /api/tasks/{taskId}} (UpdateTaskRequest em openapi.yaml). Os setters só
 * são chamados para os campos enviados: "enviado com null" retira a descrição ou o prazo. Outros
 * campos, como o estado (operação própria, DA19), ficam em {@code unknownFields} e são recusados.
 */
class UpdateTaskRequest {

    private Long version;
    private Patch<String> title = Patch.absent();
    private Patch<String> description = Patch.absent();
    private Patch<LocalDate> dueDate = Patch.absent();
    private Patch<TaskPriority> priority = Patch.absent();
    private final Set<String> unknownFields = new LinkedHashSet<>();

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setTitle(String title) {
        this.title = Patch.of(title);
    }

    public void setDescription(String description) {
        this.description = Patch.of(description);
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = Patch.of(dueDate);
    }

    public void setPriority(TaskPriority priority) {
        this.priority = Patch.of(priority);
    }

    @JsonAnySetter
    public void setUnknownField(String field, Object value) {
        unknownFields.add(field);
    }

    UpdateTaskCommand toCommand() {
        return new UpdateTaskCommand(version, title, description, dueDate, priority, Set.copyOf(unknownFields));
    }
}
