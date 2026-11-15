package pt.matriz.projects.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso à tabela {@code projects}. Só o módulo Projetos a usa (architecture.md, secção 6).
 * Os projetos eliminados contam como inexistentes (RN32, DA29): usar sempre
 * {@link #findActiveById}, nunca {@code findById}.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long id);

    default Optional<Project> findActiveById(long id) {
        return findByIdAndDeletedAtIsNull(id);
    }
}
