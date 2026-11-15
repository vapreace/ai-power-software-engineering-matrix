package pt.matriz.common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Junta os erros de validação de um pedido para os devolver todos de uma vez, em
 * {@code errors} do Problem Details (DA16). Com um só erro, o {@code detail} é a
 * mensagem desse erro.
 */
public final class ValidationErrors {

    private final List<ApiFieldError> errors = new ArrayList<>();

    public ValidationErrors add(String field, String message) {
        errors.add(new ApiFieldError(field, message));
        return this;
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    /** Lança 400 {@code VALIDATION_ERROR} se houver erros. */
    public void throwIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String detail = errors.size() == 1 ? errors.getFirst().message() : GlobalExceptionHandler.INVALID_FIELDS;
        throw new ApiException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, detail, errors);
    }
}
