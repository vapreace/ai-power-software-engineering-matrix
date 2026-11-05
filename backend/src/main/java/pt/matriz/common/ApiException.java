package pt.matriz.common;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Recusa de uma operação, convertida numa resposta Problem Details (DA16).
 * Os módulos lançam esta exceção com o código e a mensagem em português (RNF02).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ProblemCode code;
    private final List<ApiFieldError> errors;

    public ApiException(HttpStatus status, ProblemCode code, String detail) {
        this(status, code, detail, List.of());
    }

    public ApiException(HttpStatus status, ProblemCode code, String detail, List<ApiFieldError> errors) {
        super(detail);
        this.status = status;
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    /** Erro de validação de um só campo (400 {@code VALIDATION_ERROR}). */
    public static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, message,
                List.of(new ApiFieldError(field, message)));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ProblemCode getCode() {
        return code;
    }

    public List<ApiFieldError> getErrors() {
        return errors;
    }
}
