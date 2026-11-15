package pt.matriz.tracking.application;

import java.util.List;

/** Painel inicial (Dashboard em openapi.yaml): projetos e totais desses projetos (RN33). */
public record Dashboard(List<DashboardProject> projects, TaskCounts totals) {
}
