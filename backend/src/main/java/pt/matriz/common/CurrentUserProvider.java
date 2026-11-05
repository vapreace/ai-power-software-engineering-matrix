package pt.matriz.common;

/**
 * Dá o utilizador atual aos módulos. A implementação é do módulo Acesso, por isso
 * nenhum módulo precisa de depender do Acesso para saber quem faz o pedido.
 */
public interface CurrentUserProvider {

    /** O utilizador do pedido atual. Sem sessão válida, lança 401 {@code UNAUTHENTICATED}. */
    CurrentUser get();
}
