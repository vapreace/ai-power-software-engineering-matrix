package pt.matriz.projects;

import java.time.LocalDate;

/** Dados para criar um projeto (CreateProjectRequest em openapi.yaml, US04). */
public record CreateProjectCommand(
        Long teamId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate expectedEndDate,
        Long managerId) {
}
