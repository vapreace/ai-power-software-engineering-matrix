package pt.matriz.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import pt.matriz.accounts.CredentialCheck.Result;
import pt.matriz.accounts.persistence.UserAccount;
import pt.matriz.accounts.persistence.UserAccountRepository;
import pt.matriz.common.ApiException;
import pt.matriz.common.Role;

/**
 * Testes unitários de {@link Accounts} (passo 2b, DA37): email normalizado (RN04, DA10), verificação
 * de credenciais com a mesma resposta para emails que existem e que não existem (SEG04, SEG05) e
 * palavra-passe nova definida pela própria pessoa (DA25).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountsRulesTests {

    private static final String DUMMY_HASH = "hash-de-comparacao";

    @Mock
    private UserAccountRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private Accounts accounts;
    private UserAccount pedro;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("matriz-hash-de-comparacao")).thenReturn(DUMMY_HASH);
        accounts = new Accounts(repository, passwordEncoder);
        pedro = UserAccount.newAccount("Pedro", "pedro@empresa.example", "hash-pedro", Role.MEMBER);
        ReflectionTestUtils.setField(pedro, "id", 6L);
        when(repository.findByEmail("pedro@empresa.example")).thenReturn(Optional.of(pedro));
        when(passwordEncoder.matches("certa-e-comprida", "hash-pedro")).thenReturn(true);
    }

    @Test
    void emailIsStoredWithoutSpacesAndInLowercase() {
        assertThat(Accounts.normalizeEmail("  Pedro@Empresa.EXAMPLE ")).isEqualTo("pedro@empresa.example");
        assertThat(Accounts.normalizeEmail(null)).isEmpty();
    }

    @Test
    void correctCredentialsAreValidAndTheEmailIgnoresCase() {
        CredentialCheck check = accounts.checkCredentials(" PEDRO@empresa.example", "certa-e-comprida");
        assertThat(check.result()).isEqualTo(Result.VALID);
        assertThat(check.account().id()).isEqualTo(6L);
    }

    @Test
    void wrongPasswordIsInvalid() {
        assertThat(accounts.checkCredentials("pedro@empresa.example", "errada").result()).isEqualTo(Result.INVALID);
        assertThat(accounts.checkCredentials("pedro@empresa.example", null).result()).isEqualTo(Result.INVALID);
    }

    @Test
    void unknownEmailIsInvalidAndStillComparesAPassword() {
        // SEG04: compara com um hash fixo para a resposta demorar o mesmo.
        assertThat(accounts.checkCredentials("ninguem@empresa.example", "qualquer").result()).isEqualTo(Result.INVALID);
        verify(passwordEncoder).matches("qualquer", DUMMY_HASH);
    }

    @Test
    void deactivatedAccountIsOnlyIndicatedWithTheCorrectPassword() {
        // SEG05.
        pedro.changeStatus(AccountStatus.DEACTIVATED);
        assertThat(accounts.checkCredentials("pedro@empresa.example", "errada").result()).isEqualTo(Result.INVALID);
        assertThat(accounts.checkCredentials("pedro@empresa.example", "certa-e-comprida").result())
                .isEqualTo(Result.DEACTIVATED);
    }

    @Test
    void readingAccountsNeverReturnsThePassword() {
        when(repository.findById(6L)).thenReturn(Optional.of(pedro));
        when(repository.findAllById(List.of(6L))).thenReturn(List.of(pedro));
        assertThat(accounts.findById(6L)).map(AccountView::email).contains("pedro@empresa.example");
        assertThat(accounts.findAllById(List.of(6L))).extracting(AccountView::name).containsExactly("Pedro");
        assertThat(AccountView.class.getRecordComponents()).extracting(c -> c.getName()).doesNotContain("passwordHash");
    }

    @Test
    void noIdentifiersMeansNoQuery() {
        assertThat(accounts.findAllById(List.of())).isEmpty();
        verify(repository, never()).findAllById(any());
    }

    @Test
    void newPasswordIsNoLongerTemporary() {
        when(repository.findById(6L)).thenReturn(Optional.of(pedro));
        when(passwordEncoder.encode("nova-palavra-passe")).thenReturn("hash-nova");

        accounts.setNewPassword(6L, "nova-palavra-passe");

        assertThat(pedro.getPasswordHash()).isEqualTo("hash-nova");
        assertThat(pedro.isMustChangePassword()).isFalse();
    }

    @Test
    void newPasswordOfAnUnknownAccountIsNotFound() {
        ApiException error = assertThrows(ApiException.class, () -> accounts.setNewPassword(404L, "nova-palavra-passe"));
        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(passwordEncoder, never()).encode(eq("nova-palavra-passe"));
    }
}
