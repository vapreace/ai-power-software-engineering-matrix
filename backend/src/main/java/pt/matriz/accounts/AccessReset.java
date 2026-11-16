package pt.matriz.accounts;

/**
 * Evento interno publicado quando o Administrador repõe o acesso de uma conta (US02, DA25).
 * O Acesso limpa as tentativas falhadas desse email (SEG09 c).
 */
public record AccessReset(String email) {
}
