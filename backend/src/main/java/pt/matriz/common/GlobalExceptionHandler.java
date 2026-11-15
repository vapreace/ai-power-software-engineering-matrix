package pt.matriz.common;

import java.util.List;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte os erros no formato Problem Details (RFC 9457), com as extensões
 * {@code code} e {@code errors} (DA16). As mensagens estão em português (RNF02).
 * Também é usado pelos filtros de segurança, através do HandlerExceptionResolver.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String INVALID_FIELDS = "Há campos inválidos.";
    static final String INVALID_FORMAT = "O pedido tem um formato inválido.";
    static final String INVALID_VALUE = "Valor inválido.";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        ProblemDetail body = problem(ex.getStatus(), ex.getCode(), ex.getMessage());
        if (!ex.getErrors().isEmpty()) {
            body.setProperty("errors", ex.getErrors());
        }
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, INVALID_FIELDS);
        List<ApiFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(error.getField(),
                        error.getDefaultMessage() == null ? INVALID_FIELDS : error.getDefaultMessage()))
                .toList();
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, INVALID_FORMAT));
    }

    /** Parâmetro com um valor que não se converte (por exemplo, um estado que não existe). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, INVALID_VALUE);
        String field = ex.getPropertyName() == null ? "" : ex.getPropertyName();
        body.setProperty("errors", List.of(new ApiFieldError(field, INVALID_VALUE)));
        return ResponseEntity.badRequest().body(body);
    }

    static ProblemDetail problem(HttpStatus status, ProblemCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code.name());
        return problem;
    }
}
