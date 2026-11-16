package pt.matriz.teams;

/**
 * Evento interno publicado quando uma pessoa sai de uma equipa (US03, US18, architecture.md secção 7).
 * As Tarefas retiram a pessoa das tarefas abertas dos projetos da equipa (RN17) e os Projetos
 * dessa equipa que a pessoa gere ficam "Sem gestor" (RN22).
 */
public record TeamMemberRemoved(long teamId, long accountId) {
}
