package pt.matriz.accounts.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.common.Role;

/** Acesso à tabela {@code users}. Só o módulo Contas a usa (architecture.md, secção 6). */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByRoleAndStatus(Role role, AccountStatus status);

    long countByRoleAndStatus(Role role, AccountStatus status);

    /**
     * Bloqueia as contas com este papel e estado até ao fim da transação ({@code SELECT ... FOR UPDATE}),
     * sempre pela mesma ordem. Outro pedido que faça o mesmo espera pelo fim desta transação (F5, RN18).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.role = :role and u.status = :status order by u.id")
    List<UserAccount> lockByRoleAndStatus(@Param("role") Role role, @Param("status") AccountStatus status);
}
