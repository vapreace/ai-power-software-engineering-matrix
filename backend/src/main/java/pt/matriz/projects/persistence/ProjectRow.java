package pt.matriz.projects.persistence;

import java.time.LocalDate;

import pt.matriz.projects.ProjectStatus;

/** Linha da tabela {@code projects} sem a data de eliminação, para as leituras. */
public record ProjectRow(
        long id,
        long teamId,
        Long managerId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate expectedEndDate,
        ProjectStatus status,
        long version) {
}
