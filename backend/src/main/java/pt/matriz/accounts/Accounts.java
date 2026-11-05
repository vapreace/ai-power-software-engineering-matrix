package pt.matriz.accounts;

import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.matriz.accounts.persistence.UserAccount;
import pt.matriz.accounts.persistence.UserAccountRepository;
import pt.matriz.common.ApiException;
import pt.matriz.common.ProblemCode;

/** Interface pública do módulo Contas (architecture.md, secções 5.1 e 8). */
@Service
public class Accounts {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    Accounts(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        // Usado quando o email não existe, para a resposta demorar o mesmo (SEG04).
        this.dummyHash = passwordEncoder.encode("matriz-hash-de-comparacao");
    }

    /** Email como é guardado: sem espaços nas pontas e em minúsculas (RN04, DA10). */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Optional<AccountView> findById(long id) {
        return repository.findById(id).map(Accounts::toView);
    }

    /** Verifica email e palavra-passe (RF01, SEG04, SEG05). */
    @Transactional(readOnly = true)
    public CredentialCheck checkCredentials(String email, String rawPassword) {
        Optional<UserAccount> account = repository.findByEmail(normalizeEmail(email));
        String password = rawPassword == null ? "" : rawPassword;
        if (account.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            return CredentialCheck.invalid();
        }
        UserAccount found = account.get();
        if (!passwordEncoder.matches(password, found.getPasswordHash())) {
            return CredentialCheck.invalid();
        }
        if (found.getStatus() == AccountStatus.DEACTIVATED) {
            return CredentialCheck.deactivated();
        }
        return CredentialCheck.valid(toView(found));
    }

    /** Guarda a palavra-passe nova definida pela própria pessoa: deixa de ser temporária (DA25). */
    @Transactional
    public void setNewPassword(long accountId, String rawPassword) {
        UserAccount account = repository.findById(accountId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "A conta não existe."));
        account.changePassword(passwordEncoder.encode(rawPassword), false);
    }

    static AccountView toView(UserAccount account) {
        return new AccountView(account.getId(), account.getName(), account.getEmail(), account.getRole(),
                account.getStatus(), account.isMustChangePassword());
    }
}
