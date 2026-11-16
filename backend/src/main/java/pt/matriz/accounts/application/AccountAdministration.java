package pt.matriz.accounts.application;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.matriz.accounts.AccessReset;
import pt.matriz.accounts.AccountDeactivated;
import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.Accounts;
import pt.matriz.accounts.RoleChanged;
import pt.matriz.accounts.persistence.UserAccount;
import pt.matriz.accounts.persistence.UserAccountRepository;
import pt.matriz.common.ApiException;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.Patch;
import pt.matriz.common.ProblemCode;
import pt.matriz.common.Role;
import pt.matriz.common.Texts;
import pt.matriz.common.ValidationErrors;

/**
 * Gestão de contas pelo Administrador (US02, US14, US18, RF02).
 * <p>
 * As consequências noutros módulos (RN16, RN17, RN22, RN23) são tratadas por quem reage aos
 * eventos {@link AccountDeactivated} e {@link RoleChanged}, na mesma transação (DA06).
 */
@Service
public class AccountAdministration {

    static final String ACCOUNT_NOT_FOUND = "A conta não existe.";
    static final String NO_ACCESS = "Não tem acesso a esta página.";
    static final String FORBIDDEN = "Não tem permissão para esta operação.";
    static final String NAME_INVALID = "O nome é obrigatório e tem no máximo 100 caracteres.";
    static final String EMAIL_INVALID = "O email não é válido.";
    static final String ROLE_REQUIRED = "O papel é obrigatório.";
    static final String STATUS_REQUIRED = "O estado é obrigatório.";
    static final String PASSWORD_LENGTH = "A palavra-passe tem de ter entre 12 e 128 caracteres.";
    static final String NOTHING_TO_CHANGE = "Indique pelo menos um campo a alterar.";
    static final String FIELD_NOT_EDITABLE = "Este campo não pode ser alterado.";
    static final String DUPLICATE_EMAIL = "Já existe uma conta com este email";
    static final String LAST_ADMINISTRATOR = "Tem de existir pelo menos um administrador ativo";

    static final int NAME_MAX = 100;
    static final int EMAIL_MAX = 254;
    static final int PASSWORD_MIN = 12;
    static final int PASSWORD_MAX = 128;

    /** RN04: um único "@", texto antes e um domínio com pelo menos um ponto depois. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    AccountAdministration(UserAccountRepository repository, PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    /** Lista de contas, ordenada por nome (GET /users). */
    @Transactional(readOnly = true)
    public List<AccountView> list(CurrentUser user, Set<AccountStatus> statuses, Set<Role> roles) {
        if (user.role() != Role.ADMINISTRATOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, NO_ACCESS);
        }
        Set<AccountStatus> wantedStatuses = statuses == null ? Set.of() : withoutNulls(statuses);
        Set<Role> wantedRoles = roles == null ? Set.of() : withoutNulls(roles);
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-PT"));
        collator.setStrength(Collator.PRIMARY);
        return repository.findAll().stream()
                .filter(account -> wantedStatuses.isEmpty() || wantedStatuses.contains(account.getStatus()))
                .filter(account -> wantedRoles.isEmpty() || wantedRoles.contains(account.getRole()))
                .map(AccountAdministration::toView)
                .sorted(Comparator.comparing(AccountView::name, collator).thenComparingLong(AccountView::id))
                .toList();
    }

    @Transactional
    public AccountView create(CurrentUser user, String name, String email, Role role, String temporaryPassword) {
        requireAdministrator(user);
        ValidationErrors errors = new ValidationErrors();
        String validName = validName(name, errors);
        String validEmail = validEmail(email, errors);
        if (role == null) {
            errors.add("role", ROLE_REQUIRED);
        }
        checkPassword("temporaryPassword", temporaryPassword, errors);
        errors.throwIfAny();
        if (repository.findByEmail(validEmail).isPresent()) {
            throw conflict(ProblemCode.DUPLICATE_EMAIL, DUPLICATE_EMAIL);
        }
        UserAccount account = UserAccount.newAccount(validName, validEmail, passwordEncoder.encode(temporaryPassword), role);
        return toView(save(account));
    }

    @Transactional
    public AccountView update(CurrentUser user, long accountId, Patch<String> name, Patch<String> email,
            Patch<Role> role, Set<String> unknownFields) {
        UserAccount account = findAccount(accountId);
        requireAdministrator(user);
        ValidationErrors errors = new ValidationErrors();
        unknownFields.forEach(field -> errors.add(field, FIELD_NOT_EDITABLE));
        String newName = name.present() ? validName(name.value(), errors) : account.getName();
        String newEmail = email.present() ? validEmail(email.value(), errors) : account.getEmail();
        if (role.present() && role.value() == null) {
            errors.add("role", ROLE_REQUIRED);
        }
        errors.throwIfAny();
        if (!name.present() && !email.present() && !role.present()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, NOTHING_TO_CHANGE);
        }
        if (!newEmail.equals(account.getEmail())
                && repository.findByEmail(newEmail).filter(other -> !other.getId().equals(account.getId())).isPresent()) {
            throw conflict(ProblemCode.DUPLICATE_EMAIL, DUPLICATE_EMAIL);
        }
        Role previousRole = account.getRole();
        Role newRole = role.present() ? role.value() : previousRole;
        if (previousRole == Role.ADMINISTRATOR && newRole != Role.ADMINISTRATOR && isLastActiveAdministrator(account)) {
            throw conflict(ProblemCode.LAST_ADMINISTRATOR, LAST_ADMINISTRATOR);
        }
        account.rename(newName);
        account.changeEmail(newEmail);
        account.changeRole(newRole);
        save(account);
        if (newRole != previousRole) {
            events.publishEvent(new RoleChanged(account.getId(), previousRole, newRole));
        }
        return toView(account);
    }

    @Transactional
    public AccountView changeStatus(CurrentUser user, long accountId, AccountStatus status) {
        UserAccount account = findAccount(accountId);
        requireAdministrator(user);
        if (status == null) {
            throw ApiException.invalidField("status", STATUS_REQUIRED);
        }
        if (status == account.getStatus()) {
            return toView(account);
        }
        if (status == AccountStatus.DEACTIVATED && account.getRole() == Role.ADMINISTRATOR
                && isLastActiveAdministrator(account)) {
            throw conflict(ProblemCode.LAST_ADMINISTRATOR, LAST_ADMINISTRATOR);
        }
        account.changeStatus(status);
        save(account);
        if (status == AccountStatus.DEACTIVATED) {
            // As sessões abertas terminam no pedido seguinte (SEG08, DA35).
            events.publishEvent(new AccountDeactivated(account.getId()));
        }
        return toView(account);
    }

    /** Repor o acesso: palavra-passe temporária nova (DA25). */
    @Transactional
    public void resetPassword(CurrentUser user, long accountId, String temporaryPassword) {
        UserAccount account = findAccount(accountId);
        requireAdministrator(user);
        ValidationErrors errors = new ValidationErrors();
        checkPassword("temporaryPassword", temporaryPassword, errors);
        errors.throwIfAny();
        account.changePassword(passwordEncoder.encode(temporaryPassword), true);
        save(account);
        events.publishEvent(new AccessReset(account.getEmail()));
    }

    private boolean isLastActiveAdministrator(UserAccount account) {
        return account.getStatus() == AccountStatus.ACTIVE
                && repository.countByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE) <= 1;
    }

    private UserAccount save(UserAccount account) {
        try {
            return repository.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            // Dois pedidos ao mesmo tempo com o mesmo email: a restrição única da base de dados decide (DA10).
            throw conflict(ProblemCode.DUPLICATE_EMAIL, DUPLICATE_EMAIL);
        }
    }

    private UserAccount findAccount(long accountId) {
        return repository.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, ACCOUNT_NOT_FOUND));
    }

    private static void requireAdministrator(CurrentUser user) {
        if (user.role() != Role.ADMINISTRATOR) {
            throw new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN, FORBIDDEN);
        }
    }

    private static String validName(String value, ValidationErrors errors) {
        String name = Texts.normalize(value);
        if (name == null || name.isEmpty() || Texts.length(name) > NAME_MAX) {
            errors.add("name", NAME_INVALID);
        }
        return name;
    }

    private static String validEmail(String value, ValidationErrors errors) {
        String email = Accounts.normalizeEmail(value);
        if (email.isEmpty() || Texts.length(email) > EMAIL_MAX || !EMAIL.matcher(email).matches()) {
            errors.add("email", EMAIL_INVALID);
        }
        return email;
    }

    private static void checkPassword(String field, String value, ValidationErrors errors) {
        int length = Texts.length(value);
        if (length < PASSWORD_MIN || length > PASSWORD_MAX) {
            errors.add(field, PASSWORD_LENGTH);
        }
    }

    private static <T> Set<T> withoutNulls(Set<T> values) {
        return values.stream().filter(value -> value != null).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static AccountView toView(UserAccount account) {
        return new AccountView(account.getId(), account.getName(), account.getEmail(), account.getRole(),
                account.getStatus(), account.isMustChangePassword());
    }

    private static ApiException conflict(ProblemCode code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
