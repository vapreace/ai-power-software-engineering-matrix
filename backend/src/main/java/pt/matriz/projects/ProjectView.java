package pt.matriz.projects;

import java.time.LocalDate;

import pt.matriz.common.Reference;

/**
 * Dados de um projeto que os outros módulos podem ler. {@code manager} é {@code null}
 * quando o projeto está "Sem gestor" (RN22).
 */
public record ProjectView(
        long id,
        String name,
        String description,
        Reference team,
        Reference manager,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate expectedEndDate,
        long version) {
}
