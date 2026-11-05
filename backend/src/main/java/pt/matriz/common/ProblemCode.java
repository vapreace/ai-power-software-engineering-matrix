package pt.matriz.common;

/**
 * Códigos estáveis dos erros da API (DA16, DA17). A lista é a de
 * {@code Problem.code} em openapi.yaml: um código novo entra primeiro no contrato.
 */
public enum ProblemCode {
    VALIDATION_ERROR,
    UNAUTHENTICATED,
    INVALID_CREDENTIALS,
    ACCOUNT_DEACTIVATED,
    FORBIDDEN,
    PASSWORD_CHANGE_REQUIRED,
    NOT_FOUND,
    DUPLICATE_EMAIL,
    DUPLICATE_TEAM_NAME,
    DUPLICATE_TEAM_MEMBER,
    DUPLICATE_ASSIGNEE,
    INVALID_TRANSITION,
    LAST_ADMINISTRATOR,
    INVALID_MANAGER,
    INVALID_ASSIGNEE,
    INVALID_TEAM_MEMBER,
    TEAM_WITHOUT_MANAGER,
    PROJECT_NOT_ACTIVE,
    PROJECT_CLOSED,
    VERSION_CONFLICT,
    TOO_MANY_ATTEMPTS
}
