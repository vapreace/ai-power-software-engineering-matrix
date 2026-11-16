package pt.matriz.access.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import pt.matriz.accounts.AccessReset;
import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.Accounts;
import pt.matriz.accounts.CredentialCheck;
import pt.matriz.accounts.CredentialCheck.Result;
import pt.matriz.common.ApiException;
import pt.matriz.common.ApiFieldError;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.ProblemCode;
import pt.matriz.common.Role;

/**
 * Testes unitários de {@link SessionService} e {@link AccessResetListener} (passo 2b, DA37):
 * entrar (US01, SEG04, SEG05), limite de tentativas (SEG09, DA26) e palavra-passe nova
 * obrigatória depois de uma temporária (DA25, DA26).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionRulesTests {

    private static final String ADDRESS = "10.0.0.7";
    private static final String KEY = "pedro@empresa.example|" + ADDRESS;
    private static final AccountView PEDRO = new AccountView(6L, "Pedro", "pedro@empresa.example", Role.MEMBER,
            AccountStatus.ACTIVE, false);

    @Mock
    private Accounts accounts;
    @Mock
    private CurrentUsers currentUsers;
    @Mock
    private LoginAttempts attempts;

    private SessionService sessions;

    @BeforeEach
    void setUp() {
        sessions = new SessionService(accounts, currentUsers, attempts);
    }

    @Test
    void correctCredentialsEnterAndClearTheFailures() {
        CurrentUser current = new CurrentUser(6L, "Pedro", "pedro@empresa.example", Role.MEMBER, List.of(), false);
        when(accounts.checkCredentials(" Pedro@empresa.example", "certa")).thenReturn(check(Result.VALID, PEDRO));
        when(currentUsers.from(PEDRO)).thenReturn(current);

        assertThat(sessions.login(" Pedro@empresa.example", "certa", ADDRESS)).isEqualTo(current);
        verify(attempts).reset(KEY);
    }

    @Test
    void wrongCredentialsCountAsAFailureWithTheSameMessage() {
        when(accounts.checkCredentials("pedro@empresa.example", "errada")).thenReturn(check(Result.INVALID, null));

        ApiException error = assertThrows(ApiException.class, () -> sessions.login("pedro@empresa.example", "errada", ADDRESS));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(error.getCode()).isEqualTo(ProblemCode.INVALID_CREDENTIALS);
        assertThat(error.getMessage()).isEqualTo("Email ou palavra-passe incorretos");
        verify(attempts).recordFailure(KEY);
    }

    @Test
    void deactivatedAccountIsIndicatedAndDoesNotCountAsAFailure() {
        when(accounts.checkCredentials("pedro@empresa.example", "certa")).thenReturn(check(Result.DEACTIVATED, null));

        ApiException error = assertThrows(ApiException.class, () -> sessions.login("pedro@empresa.example", "certa", ADDRESS));

        assertThat(error.getCode()).isEqualTo(ProblemCode.ACCOUNT_DEACTIVATED);
        assertThat(error.getMessage()).isEqualTo("Conta desativada. Contacte o administrador.");
        verify(attempts, never()).recordFailure(anyString());
    }

    @Test
    void blockedPairDoesNotEvenCheckTheCredentials() {
        when(attempts.isBlocked(KEY)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> sessions.login("pedro@empresa.example", "certa", ADDRESS));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(error.getCode()).isEqualTo(ProblemCode.TOO_MANY_ATTEMPTS);
        verify(accounts, never()).checkCredentials(anyString(), anyString());
    }

    @Test
    void onlyWhoHasATemporaryPasswordDefinesANewOneHere() {
        ApiException error = assertThrows(ApiException.class,
                () -> sessions.changeTemporaryPassword(user(false), "nova-palavra-passe"));
        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(accounts, never()).setNewPassword(anyLong(), anyString());
    }

    @ParameterizedTest(name = "palavra-passe nova [{0}]")
    @NullSource
    @ValueSource(strings = {"", "onze-letras", "palavra-com-mais-de-cento-e-vinte-e-oito-caracteres-palavra-com-mais-de-cento-e-vinte-e-oito-caracteres-palavra-com-mais-de-cento"})
    void newPasswordHas12To128Characters(String password) {
        ApiException error = assertThrows(ApiException.class, () -> sessions.changeTemporaryPassword(user(true), password));
        assertThat(error.getErrors()).containsExactly(
                new ApiFieldError("newPassword", "A palavra-passe tem de ter entre 12 e 128 caracteres."));
    }

    @Test
    void validNewPasswordIsSaved() {
        sessions.changeTemporaryPassword(user(true), "doze-letras!");
        sessions.changeTemporaryPassword(user(true), "p".repeat(128));
        verify(accounts).setNewPassword(6L, "doze-letras!");
        verify(accounts).setNewPassword(6L, "p".repeat(128));
    }

    @Test
    void resetAccessClearsTheAttemptsOfThatEmail() {
        new AccessResetListener(attempts).on(new AccessReset("pedro@empresa.example"));
        verify(attempts).clearEmail("pedro@empresa.example");
    }

    private static CredentialCheck check(Result result, AccountView account) {
        return new CredentialCheck(result, account);
    }

    private static CurrentUser user(boolean mustChangePassword) {
        return new CurrentUser(6L, "Pedro", "pedro@empresa.example", Role.MEMBER, List.of(), mustChangePassword);
    }
}
