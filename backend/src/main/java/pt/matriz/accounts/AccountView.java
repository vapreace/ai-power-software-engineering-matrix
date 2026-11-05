package pt.matriz.accounts;

import pt.matriz.common.Role;

/** Dados de uma conta que os outros módulos podem ler. Nunca inclui a palavra-passe (SEG03). */
public record AccountView(
        long id,
        String name,
        String email,
        Role role,
        AccountStatus status,
        boolean mustChangePassword) {
}
