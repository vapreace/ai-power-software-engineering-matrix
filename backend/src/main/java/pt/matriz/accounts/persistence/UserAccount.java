package pt.matriz.accounts.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.common.Role;

/** Tabela {@code users} (database.md, secção 3.1). */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    protected UserAccount() {
    }

    /** Conta nova: ativa e com palavra-passe temporária (DA25). */
    public static UserAccount newAccount(String name, String email, String passwordHash, Role role) {
        UserAccount account = new UserAccount();
        account.name = name;
        account.email = email;
        account.passwordHash = passwordHash;
        account.mustChangePassword = true;
        account.role = role;
        account.status = AccountStatus.ACTIVE;
        return account;
    }

    public void changePassword(String passwordHash, boolean temporary) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = temporary;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
