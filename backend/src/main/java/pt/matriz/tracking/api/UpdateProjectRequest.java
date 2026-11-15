package pt.matriz.tracking.api;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import pt.matriz.common.Patch;
import pt.matriz.projects.UpdateProjectCommand;

/**
 * Pedido de {@code PATCH /api/projects/{projectId}} (UpdateProjectRequest em openapi.yaml).
 * Os setters só são chamados para os campos enviados, por isso distingue "não enviado"
 * de "enviado com null" (retirar a descrição ou a data de fim prevista). Outros campos,
 * como a equipa, ficam em {@code unknownFields} e são recusados (RN21).
 */
class UpdateProjectRequest {

    private Long version;
    private Patch<String> name = Patch.absent();
    private Patch<String> description = Patch.absent();
    private Patch<LocalDate> startDate = Patch.absent();
    private Patch<LocalDate> expectedEndDate = Patch.absent();
    private final Set<String> unknownFields = new LinkedHashSet<>();

    public void setVersion(Long version) {
        this.version = version;
    }

    public void setName(String name) {
        this.name = Patch.of(name);
    }

    public void setDescription(String description) {
        this.description = Patch.of(description);
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = Patch.of(startDate);
    }

    public void setExpectedEndDate(LocalDate expectedEndDate) {
        this.expectedEndDate = Patch.of(expectedEndDate);
    }

    @JsonAnySetter
    public void setUnknownField(String field, Object value) {
        unknownFields.add(field);
    }

    UpdateProjectCommand toCommand() {
        return new UpdateProjectCommand(version, name, description, startDate, expectedEndDate, Set.copyOf(unknownFields));
    }
}
