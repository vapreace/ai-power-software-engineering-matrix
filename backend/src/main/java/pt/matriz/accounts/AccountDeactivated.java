package pt.matriz.accounts;

/**
 * Evento interno publicado quando uma conta é desativada (US14, architecture.md secção 7).
 * As Tarefas retiram a pessoa das tarefas abertas (RN16) e os Projetos ficam "Sem gestor" (RN22).
 */
public record AccountDeactivated(long accountId) {
}
