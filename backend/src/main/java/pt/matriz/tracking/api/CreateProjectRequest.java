package pt.matriz.tracking.api;

import java.time.LocalDate;

import pt.matriz.projects.CreateProjectCommand;

/**
 * Pedido de {@code POST /api/projects} (CreateProjectRequest em openapi.yaml). A validação é
 * feita pelo módulo Projetos, depois das permissões, para seguir a ordem de DA17.
 */
record CreateProjectRequest(
        Long teamId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate expectedEndDate,
        Long managerId) {

    CreateProjectCommand toCommand() {
        return new CreateProjectCommand(teamId, name, description, startDate, expectedEndDate, managerId);
    }
}
