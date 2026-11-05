package pt.matriz.access.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import pt.matriz.accounts.Accounts;
import pt.matriz.accounts.CredentialCheck;
import pt.matriz.common.ApiException;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.ProblemCode;

/** Casos de uso da sessão: entrar e definir a palavra-passe nova (US01). */
@Service
public class SessionService {

    public static final String SESSION_ENDED = "A sessão terminou. Entre de novo.";
    static final String INVALID_CREDENTIALS = "Email ou palavra-passe incorretos";
    static final String ACCOUNT_DEACTIVATED = "Conta desativada. Contacte o administrador.";
    static final String TOO_MANY_ATTEMPTS = "Demasiadas tentativas. Tente de novo dentro de 15 minutos.";
    static final String PASSWORD_LENGTH = "A palavra-passe tem de ter entre 12 e 128 caracteres.";
    static final String FORBIDDEN = "Não tem permissão para esta operação.";

    private final Accounts accounts;
    private final CurrentUsers currentUsers;
    private final LoginAttempts attempts;

    SessionService(Accounts accounts, CurrentUsers currentUsers, LoginAttempts attempts) {
        this.accounts = accounts;
        this.currentUsers = currentUsers;
        this.attempts = attempts;
    }

    /**
     * Verifica as credenciais (RF01). A mensagem é igual para emails que existem e que
     * não existem (SEG04). A conta desativada só é indicada com a palavra-passe correta (SEG05).
     */
    public CurrentUser login(String email, String password, String remoteAddress) {
        String key = LoginAttempts.key(Accounts.normalizeEmail(email), remoteAddress);
        if (attempts.isBlocked(key)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, ProblemCode.TOO_MANY_ATTEMPTS, TOO_MANY_ATTEMPTS);
        }
        CredentialCheck check = accounts.checkCredentials(email, password);
        return switch (check.result()) {
            case INVALID -> {
                attempts.recordFailure(key);
                throw new ApiException(HttpStatus.UNAUTHORIZED, ProblemCode.INVALID_CREDENTIALS, INVALID_CREDENTIALS);
            }
            case DEACTIVATED ->
                throw new ApiException(HttpStatus.UNAUTHORIZED, ProblemCode.ACCOUNT_DEACTIVATED, ACCOUNT_DEACTIVATED);
            case VALID -> {
                attempts.reset(key);
                yield currentUsers.from(check.account());
            }
        };
    }

    /** Substitui a palavra-passe temporária (DA25). A nova tem 12 a 128 caracteres (DA26). */
    public void changeTemporaryPassword(CurrentUser user, String newPassword) {
        if (!user.mustChangePassword()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, FORBIDDEN);
        }
        int length = newPassword == null ? 0 : newPassword.codePointCount(0, newPassword.length());
        if (length < 12 || length > 128) {
            throw ApiException.invalidField("newPassword", PASSWORD_LENGTH);
        }
        accounts.setNewPassword(user.id(), newPassword);
    }
}
