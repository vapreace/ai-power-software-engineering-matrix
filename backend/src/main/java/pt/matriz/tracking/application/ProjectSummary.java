package pt.matriz.tracking.application;

import java.time.LocalDate;

import pt.matriz.common.Reference;
import pt.matriz.projects.ProjectStatus;

/** Linha da lista de projetos (ProjectSummary em openapi.yaml, RF09). */
public record ProjectSummary(
        long id,
        String name,
        Reference team,
        Reference manager,
        ProjectStatus status,
        LocalDate expectedEndDate,
        Progress progress,
        long overdueTaskCount) {
}
