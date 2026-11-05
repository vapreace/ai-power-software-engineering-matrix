package pt.matriz.common;

/** Erro de um campo, no array {@code errors} do Problem Details (DA16). */
public record ApiFieldError(String field, String message) {
}
