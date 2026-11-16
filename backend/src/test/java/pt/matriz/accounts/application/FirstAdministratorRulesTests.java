package pt.matriz.accounts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.persistence.UserAccount;
import pt.matriz.accounts.persistence.UserAccountRepository;
import pt.matriz.common.Role;

/**
 * Testes unitários de {@link FirstAdministratorInitializer} (passo 2b, DA37): a primeira conta de
 * Administrador nasce da configuração, com palavra-passe temporária, só quando não há nenhum
 * Administrador ativo (DA25, RN18).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FirstAdministratorRulesTests {

    private static final String PASSWORD = "troque-esta-palavra";

    @Mock
    private UserAccountRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenAnswer(call -> "hash:" + call.getArgument(0));
    }

    @Test
    void nothingIsCreatedWhenAnActiveAdministratorExists() {
        when(repository.existsByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE)).thenReturn(true);
        initializer("admin@empresa.example", PASSWORD).run(null);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest(name = "email [{0}] e palavra-passe [{1}]")
    @CsvSource({"'', troque-esta-palavra", "admin@empresa.example, ''", "'  ', '  '"})
    void nothingIsCreatedWithoutEmailOrPassword(String email, String password) {
        initializer(email, password).run(null);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest(name = "palavra-passe com {0} caracteres")
    @ValueSource(ints = {11, 129})
    void passwordOutsideTheLimitsStopsTheStartup(int length) {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> initializer("admin@empresa.example", "p".repeat(length)).run(null));
        assertThat(error.getMessage()).contains("12 e 128");
    }

    @Test
    void emailOfAnotherAccountIsNotReused() {
        when(repository.findByEmail("admin@empresa.example")).thenReturn(Optional.of(
                UserAccount.newAccount("Outra", "admin@empresa.example", "hash", Role.MEMBER)));
        initializer("Admin@Empresa.example", PASSWORD).run(null);
        verify(repository, never()).save(any());
    }

    @Test
    void firstAdministratorIsCreatedWithATemporaryPassword() {
        initializer("  Admin@Empresa.example ", PASSWORD).run(null);

        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Administrador");
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@empresa.example");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash:" + PASSWORD);
        assertThat(saved.getValue().isMustChangePassword()).isTrue();
    }

    private FirstAdministratorInitializer initializer(String email, String password) {
        return new FirstAdministratorInitializer(repository, passwordEncoder, "  Administrador ", email, password);
    }
}
