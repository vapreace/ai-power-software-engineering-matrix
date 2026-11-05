package pt.matriz.common;

import org.springframework.http.HttpStatus;

/**
 * Recusa de uma operação, convertida numa resposta Problem Details (DA16).
 * Os módulos lançam esta exceção com o código e a mensagem em português (RNF02).
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ProblemCode code;

    public ApiException(HttpStatus status, ProblemCode code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ProblemCode getCode() {
        return code;
    }
}
