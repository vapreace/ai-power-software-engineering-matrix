package pt.matriz.accounts.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.common.Role;

/** Acesso à tabela {@code users}. Só o módulo Contas a usa (architecture.md, secção 6). */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByRoleAndStatus(Role role, AccountStatus status);

    long countByRoleAndStatus(Role role, AccountStatus status);
}
