package pt.matriz.access.api;

import jakarta.validation.constraints.NotBlank;

/** Pedido de {@code POST /api/session} (LoginRequest em openapi.yaml). */
record LoginRequest(
        @NotBlank(message = "O email é obrigatório.") String email,
        @NotBlank(message = "A palavra-passe é obrigatória.") String password) {
}
