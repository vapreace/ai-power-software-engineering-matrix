package pt.matriz.tracking.application;

import pt.matriz.common.Reference;
import pt.matriz.projects.ProjectStatus;

/** Projeto do painel inicial (DashboardProject em openapi.yaml 0.3.1, US21). */
public record DashboardProject(
        long id,
        String name,
        Reference team,
        Reference manager,
        ProjectStatus status,
        Progress progress,
        TaskCounts taskCounts) {
}
