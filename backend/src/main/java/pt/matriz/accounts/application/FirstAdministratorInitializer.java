package pt.matriz.accounts.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.Accounts;
import pt.matriz.accounts.persistence.UserAccount;
import pt.matriz.accounts.persistence.UserAccountRepository;
import pt.matriz.common.Role;

/**
 * Cria a primeira conta de Administrador no arranque, a partir da configuração,
 * com palavra-passe temporária (DA25). Sem ela, RN18 não se cumpre. Só atua se
 * ainda não existir nenhum Administrador ativo.
 */
@Component
class FirstAdministratorInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FirstAdministratorInitializer.class);

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String email;
    private final String password;

    FirstAdministratorInitializer(UserAccountRepository repository, PasswordEncoder passwordEncoder,
            @Value("${matriz.first-administrator.name:Administrador}") String name,
            @Value("${matriz.first-administrator.email:}") String email,
            @Value("${matriz.first-administrator.password:}") String password) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.existsByRoleAndStatus(Role.ADMINISTRATOR, AccountStatus.ACTIVE)) {
            return;
        }
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("Não existe nenhum Administrador ativo. Defina MATRIZ_ADMIN_EMAIL e MATRIZ_ADMIN_PASSWORD no .env (DA25).");
            return;
        }
        int length = password.codePointCount(0, password.length());
        if (length < 12 || length > 128) {
            throw new IllegalStateException("MATRIZ_ADMIN_PASSWORD tem de ter entre 12 e 128 caracteres (DA26).");
        }
        String normalizedEmail = Accounts.normalizeEmail(email);
        if (repository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("O email de MATRIZ_ADMIN_EMAIL já pertence a uma conta que não é Administrador ativo. Nada foi criado.");
            return;
        }
        repository.save(UserAccount.newAccount(name.strip(), normalizedEmail, passwordEncoder.encode(password), Role.ADMINISTRATOR));
        log.info("Criada a conta do primeiro Administrador, com palavra-passe temporária (DA25).");
    }
}
