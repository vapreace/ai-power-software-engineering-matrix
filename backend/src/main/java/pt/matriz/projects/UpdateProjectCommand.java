package pt.matriz.projects;

import java.time.LocalDate;
import java.util.Set;

import pt.matriz.common.Patch;

/**
 * Dados para editar um projeto (UpdateProjectRequest em openapi.yaml, US17). Só os campos
 * enviados mudam. {@code unknownFields} são campos que não se editam aqui, como a equipa (RN21).
 */
public record UpdateProjectCommand(
        Long version,
        Patch<String> name,
        Patch<String> description,
        Patch<LocalDate> startDate,
        Patch<LocalDate> expectedEndDate,
        Set<String> unknownFields) {

    boolean changesSomething() {
        return name.present() || description.present() || startDate.present() || expectedEndDate.present()
                || !unknownFields.isEmpty();
    }
}
