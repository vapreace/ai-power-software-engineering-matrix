package pt.matriz.tasks.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import pt.matriz.projects.ProjectDeleted;
import pt.matriz.tasks.persistence.TaskQueries;

/**
 * Reage a "Projeto eliminado" (architecture.md, secção 7): as tarefas do projeto ficam
 * eliminadas na mesma transação, com a mesma data e hora. É uma operação interna, sem a
 * verificação de RN09, porque se aplica em qualquer estado do projeto (E45).
 */
@Component
class ProjectDeletedListener {

    private final TaskQueries queries;

    ProjectDeletedListener(TaskQueries queries) {
        this.queries = queries;
    }

    @EventListener
    void on(ProjectDeleted event) {
        queries.markDeletedByProject(event.projectId(), event.deletedAt());
    }
}
