package pt.matriz.common;

import java.util.List;

/**
 * Quem faz o pedido (architecture.md, secção 5.2). É lido da base de dados em cada
 * pedido e nunca vem do cliente (SEG02). Tem a forma de CurrentUser em openapi.yaml.
 */
public record CurrentUser(
        long id,
        String name,
        String email,
        Role role,
        List<Reference> teams,
        boolean mustChangePassword) {
}
