package pt.matriz.accounts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import pt.matriz.accounts.AccessReset;
import pt.matriz.accounts.AccountDeactivated;
import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.RoleChanged;
import pt.matriz.accounts.persistence.UserAccount;
import pt.matriz.accounts.persistence.UserAccountRepository;
import pt.matriz.common.ApiException;
import pt.matriz.common.ApiFieldError;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.Patch;
import pt.matriz.common.ProblemCode;
import pt.matriz.common.Role;

/**
 * Testes unitários das regras de {@link AccountAdministration} (passo 2b, DA37): só o Administrador
 * (RF02), validações (RN04, RN29, DA26), email único (RN04, DA10), último Administrador (RN18) e os
 * eventos que levam as consequências aos outros módulos (RN16, RN23, DA25).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountAdministrationRulesTests {

    private static final long PEDRO_ID = 6L;
    private static final String PASSWORD = "palavra-temporaria";
    private static final CurrentUser ANA = user(Role.ADMINISTRATOR);
    private static final CurrentUser RUI = user(Role.MANAGER);

    @Mock
    private UserAccountRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationEventPublisher events;

    private AccountAdministration administration;

    @BeforeEach
    void setUp() {
        administration = new AccountAdministration(repository, passwordEncoder, events);
        when(passwordEncoder.encode(anyString())).thenAnswer(call -> "hash:" + call.getArgument(0));
        when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(call -> {
            UserAccount saved = call.getArgument(0);
            if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", 99L);
            }
            return saved;
        });
        when(repository.countByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE)).thenReturn(2L);
    }

    // ───── Lista (RF02) ─────

    @Nested
    class Listing {

        @Test
        void onlyTheAdministratorSeesTheAccounts() {
            ApiException error = refused(() -> administration.list(RUI, null, null));
            assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(error.getMessage()).isEqualTo("Não tem acesso a esta página.");
        }

        @Test
        void accountsAreOrderedByNameWithoutAccentsAndFiltered() {
            when(repository.findAll()).thenReturn(List.of(
                    account(3L, "Rui", Role.MANAGER, AccountStatus.ACTIVE),
                    account(1L, "Álvaro", Role.MEMBER, AccountStatus.DEACTIVATED),
                    account(2L, "ana", Role.ADMINISTRATOR, AccountStatus.ACTIVE)));

            assertThat(administration.list(ANA, null, null)).extracting(AccountView::id).containsExactly(1L, 2L, 3L);
            assertThat(administration.list(ANA, Set.of(AccountStatus.ACTIVE), Set.of()))
                    .extracting(AccountView::id).containsExactly(2L, 3L);
            assertThat(administration.list(ANA, new HashSet<>(Arrays.asList((AccountStatus) null)), Set.of(Role.MANAGER)))
                    .extracting(AccountView::id).containsExactly(3L);
        }
    }

    // ───── Criar (US02) ─────

    @Nested
    class Creating {

        @Test
        void onlyTheAdministratorCreatesAccounts() {
            assertThat(refused(() -> administration.create(RUI, "Joana", "joana@empresa.example", Role.MEMBER, PASSWORD))
                    .getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void newAccountIsActiveWithALowercaseEmailAndATemporaryPassword() {
            AccountView created = administration.create(ANA, "  Joana  ", " Joana@Empresa.Example ", Role.MEMBER, PASSWORD);

            assertThat(created.name()).isEqualTo("Joana");
            assertThat(created.email()).isEqualTo("joana@empresa.example");
            assertThat(created.status()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(created.mustChangePassword()).isTrue();
            ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
            verify(repository).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash:" + PASSWORD);
        }

        @ParameterizedTest(name = "email inválido: [{0}]")
        @NullSource
        @ValueSource(strings = {"", "joana", "joana@empresa", "joana@@empresa.example", "jo ana@empresa.example",
                "@empresa.example", "joana@empresa..example"})
        void emailMustHaveTheFormNameAtDomain(String email) {
            ApiException error = refused(() -> administration.create(ANA, "Joana", email, Role.MEMBER, PASSWORD));
            assertThat(error.getErrors()).containsExactly(new ApiFieldError("email", "O email não é válido."));
        }

        @Test
        void emailHasAtMost254Characters() {
            String email254 = "a".repeat(254 - "@empresa.example".length()) + "@empresa.example";
            assertThat(administration.create(ANA, "Joana", email254, Role.MEMBER, PASSWORD).email()).hasSize(254);
            ApiException error = refused(() -> administration.create(ANA, "Joana", "a" + email254, Role.MEMBER, PASSWORD));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("email");
        }

        @Test
        void allInvalidFieldsAreReturnedTogether() {
            ApiException error = refused(() -> administration.create(ANA, "n".repeat(101), "x", null, "curta"));
            assertThat(error.getErrors()).extracting(ApiFieldError::field)
                    .containsExactly("name", "email", "role", "temporaryPassword");
        }

        @ParameterizedTest(name = "palavra-passe com {0} caracteres")
        @ValueSource(ints = {0, 11, 129})
        void temporaryPasswordHas12To128Characters(int length) {
            ApiException error = refused(() -> administration.create(ANA, "Joana", "joana@empresa.example", Role.MEMBER,
                    "p".repeat(length)));
            assertThat(error.getErrors()).containsExactly(new ApiFieldError("temporaryPassword",
                    "A palavra-passe tem de ter entre 12 e 128 caracteres."));
        }

        @Test
        void passwordOf12And128CharactersIsAccepted() {
            administration.create(ANA, "Joana", "joana@empresa.example", Role.MEMBER, "p".repeat(12));
            administration.create(ANA, "Joana", "joana2@empresa.example", Role.MEMBER, "p".repeat(128));
            verify(repository, org.mockito.Mockito.times(2)).saveAndFlush(any());
        }

        @Test
        void nameIsRequired() {
            ApiException error = refused(() -> administration.create(ANA, null, "joana@empresa.example", Role.MEMBER, PASSWORD));
            assertThat(error.getErrors()).containsExactly(
                    new ApiFieldError("name", "O nome é obrigatório e tem no máximo 100 caracteres."));
        }

        @Test
        void missingPasswordIsRefused() {
            assertThat(refused(() -> administration.create(ANA, "Joana", "joana@empresa.example", Role.MEMBER, null))
                    .getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void emailThatAlreadyExistsIsRefused() {
            when(repository.findByEmail("joana@empresa.example")).thenReturn(Optional.of(
                    account(7L, "Joana", Role.MEMBER, AccountStatus.ACTIVE)));
            ApiException error = refused(() -> administration.create(ANA, "Joana", "JOANA@empresa.example", Role.MEMBER, PASSWORD));
            assertThat(error.getCode()).isEqualTo(ProblemCode.DUPLICATE_EMAIL);
        }

        @Test
        void simultaneousCreationWithTheSameEmailIsADuplicate() {
            when(repository.saveAndFlush(any(UserAccount.class))).thenThrow(new DataIntegrityViolationException("uk_users_email"));
            assertThat(refused(() -> administration.create(ANA, "Joana", "joana@empresa.example", Role.MEMBER, PASSWORD))
                    .getCode()).isEqualTo(ProblemCode.DUPLICATE_EMAIL);
        }
    }

    // ───── Editar e mudar o papel (US02, US18, RN18, RN23) ─────

    @Nested
    class Editing {

        private UserAccount pedro;

        @BeforeEach
        void existingAccount() {
            pedro = givenAccount(PEDRO_ID, "Pedro", Role.MEMBER, AccountStatus.ACTIVE);
        }

        @Test
        void unknownAccountIsNotFound() {
            assertThat(refused(() -> administration.update(ANA, 404L, Patch.of("X"), absent(), absent(), Set.of()))
                    .getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void onlyTheAdministratorEdits() {
            assertThat(refused(() -> administration.update(RUI, PEDRO_ID, Patch.of("X"), absent(), absent(), Set.of()))
                    .getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void fieldsThatAreNotEditedHereAndInvalidValuesAreRefusedTogether() {
            ApiException error = refused(() -> administration.update(ANA, PEDRO_ID, Patch.of(" "), Patch.of("x"),
                    Patch.of(null), Set.of("status")));
            assertThat(error.getErrors()).extracting(ApiFieldError::field).containsExactly("status", "name", "email", "role");
        }

        @Test
        void atLeastOneFieldMustChange() {
            ApiException error = refused(() -> administration.update(ANA, PEDRO_ID, absent(), absent(), absent(), Set.of()));
            assertThat(error.getMessage()).isEqualTo("Indique pelo menos um campo a alterar.");
        }

        @Test
        void newEmailCannotBelongToAnotherAccount() {
            when(repository.findByEmail("marta@empresa.example")).thenReturn(Optional.of(
                    account(5L, "Marta", Role.MEMBER, AccountStatus.ACTIVE)));
            assertThat(refused(() -> administration.update(ANA, PEDRO_ID, absent(), Patch.of("Marta@empresa.example"), absent(),
                    Set.of())).getCode()).isEqualTo(ProblemCode.DUPLICATE_EMAIL);
        }

        @Test
        void keepingTheSameEmailOrFindingTheSameAccountIsNotADuplicate() {
            assertThat(administration.update(ANA, PEDRO_ID, Patch.of("Pedro Silva"), Patch.of("PEDRO@empresa.example"),
                    absent(), Set.of()).name()).isEqualTo("Pedro Silva");
            when(repository.findByEmail("pedro.silva@empresa.example")).thenReturn(Optional.of(pedro));
            assertThat(administration.update(ANA, PEDRO_ID, absent(), Patch.of("pedro.silva@empresa.example"), absent(),
                    Set.of()).email()).isEqualTo("pedro.silva@empresa.example");
        }

        @Test
        void changingTheRolePublishesTheEvent() {
            administration.update(ANA, PEDRO_ID, absent(), absent(), Patch.of(Role.OBSERVER), Set.of());
            verify(events).publishEvent(new RoleChanged(PEDRO_ID, Role.MEMBER, Role.OBSERVER));
        }

        @Test
        void editingWithoutChangingTheRolePublishesNothing() {
            administration.update(ANA, PEDRO_ID, Patch.of("Pedro Silva"), absent(), Patch.of(Role.MEMBER), Set.of());
            verify(events, never()).publishEvent(any(Object.class));
        }

        @Test
        void lastActiveAdministratorKeepsTheRole() {
            givenAccount(1L, "Ana", Role.ADMINISTRATOR, AccountStatus.ACTIVE);
            when(repository.countByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE)).thenReturn(1L);
            ApiException error = refused(() -> administration.update(ANA, 1L, absent(), absent(), Patch.of(Role.MANAGER), Set.of()));
            assertThat(error.getCode()).isEqualTo(ProblemCode.LAST_ADMINISTRATOR);
            assertThat(error.getMessage()).isEqualTo("Tem de existir pelo menos um administrador ativo");
        }

        @Test
        void lastActiveAdministratorCanStillBeRenamed() {
            givenAccount(1L, "Ana", Role.ADMINISTRATOR, AccountStatus.ACTIVE);
            when(repository.countByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE)).thenReturn(1L);
            assertThat(administration.update(ANA, 1L, Patch.of("Ana Sousa"), absent(), Patch.of(Role.ADMINISTRATOR), Set.of())
                    .name()).isEqualTo("Ana Sousa");
        }

        @Test
        void administratorChangesRoleWhenAnotherIsActiveOrWhenTheAccountIsDeactivated() {
            givenAccount(1L, "Ana", Role.ADMINISTRATOR, AccountStatus.ACTIVE);
            assertThat(administration.update(ANA, 1L, absent(), absent(), Patch.of(Role.MANAGER), Set.of()).role())
                    .isEqualTo(Role.MANAGER);
            givenAccount(4L, "Antiga", Role.ADMINISTRATOR, AccountStatus.DEACTIVATED);
            when(repository.countByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE)).thenReturn(1L);
            assertThat(administration.update(ANA, 4L, absent(), absent(), Patch.of(Role.MEMBER), Set.of()).role())
                    .isEqualTo(Role.MEMBER);
        }
    }

    // ───── Estado e acesso (US02, US14, RN16, RN18, DA25) ─────

    @Nested
    class StatusAndAccess {

        @BeforeEach
        void existingAccount() {
            givenAccount(PEDRO_ID, "Pedro", Role.MEMBER, AccountStatus.ACTIVE);
        }

        @Test
        void onlyTheAdministratorChangesTheStatus() {
            assertThat(refused(() -> administration.changeStatus(RUI, PEDRO_ID, AccountStatus.DEACTIVATED)).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(refused(() -> administration.changeStatus(ANA, 404L, AccountStatus.DEACTIVATED)).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void statusIsRequired() {
            assertThat(refused(() -> administration.changeStatus(ANA, PEDRO_ID, null)).getErrors())
                    .containsExactly(new ApiFieldError("status", "O estado é obrigatório."));
        }

        @Test
        void sameStatusChangesNothing() {
            assertThat(administration.changeStatus(ANA, PEDRO_ID, AccountStatus.ACTIVE).status()).isEqualTo(AccountStatus.ACTIVE);
            verify(repository, never()).saveAndFlush(any());
            verify(events, never()).publishEvent(any(Object.class));
        }

        @Test
        void deactivatingPublishesTheEvent() {
            assertThat(administration.changeStatus(ANA, PEDRO_ID, AccountStatus.DEACTIVATED).status())
                    .isEqualTo(AccountStatus.DEACTIVATED);
            verify(events).publishEvent(new AccountDeactivated(PEDRO_ID));
        }

        @Test
        void reactivatingPublishesNothing() {
            // RN28: as tarefas não voltam a ser atribuídas.
            givenAccount(PEDRO_ID, "Pedro", Role.MEMBER, AccountStatus.DEACTIVATED);
            administration.changeStatus(ANA, PEDRO_ID, AccountStatus.ACTIVE);
            verify(events, never()).publishEvent(any(Object.class));
        }

        @Test
        void lastActiveAdministratorIsNotDeactivatedButAnotherCanBe() {
            givenAccount(1L, "Ana", Role.ADMINISTRATOR, AccountStatus.ACTIVE);
            when(repository.countByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE)).thenReturn(1L);
            assertThat(refused(() -> administration.changeStatus(ANA, 1L, AccountStatus.DEACTIVATED)).getCode())
                    .isEqualTo(ProblemCode.LAST_ADMINISTRATOR);
            when(repository.countByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE)).thenReturn(2L);
            assertThat(administration.changeStatus(ANA, 1L, AccountStatus.DEACTIVATED).status())
                    .isEqualTo(AccountStatus.DEACTIVATED);
        }

        @Test
        void resetAccessSetsANewTemporaryPasswordAndPublishesTheEvent() {
            administration.resetPassword(ANA, PEDRO_ID, PASSWORD);

            ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
            verify(repository).saveAndFlush(saved.capture());
            assertThat(saved.getValue().getPasswordHash()).isEqualTo("hash:" + PASSWORD);
            assertThat(saved.getValue().isMustChangePassword()).isTrue();
            verify(events).publishEvent(new AccessReset("pedro@empresa.example"));
        }

        @Test
        void resetAccessChecksPermissionAndPassword() {
            assertThat(refused(() -> administration.resetPassword(RUI, PEDRO_ID, PASSWORD)).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(refused(() -> administration.resetPassword(ANA, 404L, PASSWORD)).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(refused(() -> administration.resetPassword(ANA, PEDRO_ID, "curta")).getErrors())
                    .extracting(ApiFieldError::field).containsExactly("temporaryPassword");
        }
    }

    // ───── Dados e verificações comuns ─────

    private UserAccount givenAccount(long id, String name, Role role, AccountStatus status) {
        UserAccount account = account(id, name, role, status);
        when(repository.findById(id)).thenReturn(Optional.of(account));
        return account;
    }

    private static UserAccount account(long id, String name, Role role, AccountStatus status) {
        UserAccount account = UserAccount.newAccount(name, name.toLowerCase() + "@empresa.example", "hash", role);
        ReflectionTestUtils.setField(account, "id", id);
        account.changeStatus(status);
        return account;
    }

    private static <T> Patch<T> absent() {
        return Patch.absent();
    }

    private static CurrentUser user(Role role) {
        return new CurrentUser(role == Role.ADMINISTRATOR ? 1L : 2L, "Pessoa", "pessoa@empresa.example", role, List.of(), false);
    }

    private static ApiException refused(Executable call) {
        return assertThrows(ApiException.class, call);
    }
}
