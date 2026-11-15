package pt.matriz.tasks.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso à tabela {@code tasks}. Só o módulo Tarefas a usa (architecture.md, secção 6).
 * As tarefas eliminadas contam como inexistentes (RN32, DA29): usar sempre
 * {@link #findActiveById}, nunca {@code findById}.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndDeletedAtIsNull(Long id);

    default Optional<Task> findActiveById(long id) {
        return findByIdAndDeletedAtIsNull(id);
    }
}
