package pt.matriz.tracking.application;

import java.time.LocalDate;

import pt.matriz.common.Reference;
import pt.matriz.projects.ProjectStatus;

/** Detalhe do projeto (Project em openapi.yaml, US09, US17). */
public record ProjectDetail(
        long id,
        String name,
        Reference team,
        Reference manager,
        ProjectStatus status,
        LocalDate expectedEndDate,
        Progress progress,
        long overdueTaskCount,
        String description,
        LocalDate startDate,
        long version) {
}
