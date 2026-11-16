package pt.matriz.accounts;

import pt.matriz.common.Role;

/**
 * Evento interno publicado quando o papel de uma conta muda (US18, architecture.md secção 7).
 * Os Projetos tratam quem deixou de ser Gestor (RN22, RN23) e as Equipas tratam quem passou a
 * Observador ou Administrador (RN23, RN26).
 */
public record RoleChanged(long accountId, Role previousRole, Role newRole) {
}
