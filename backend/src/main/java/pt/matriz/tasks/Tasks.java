package pt.matriz.tasks;

import java.util.Collection;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.matriz.common.OrganizationClock;
import pt.matriz.tasks.persistence.TaskQueries;

/**
 * Interface pública do módulo Tarefas (architecture.md, secções 5.1 e 8). No I2 só dá as
 * contagens por projeto, para o progresso e os atrasos (RF27, RF28). As operações sobre
 * tarefas chegam no I3.
 */
@Service
public class Tasks {

    private final TaskQueries queries;
    private final OrganizationClock clock;

    Tasks(TaskQueries queries, OrganizationClock clock) {
        this.queries = queries;
        this.clock = clock;
    }

    /**
     * Contagens de cada projeto indicado. Um projeto sem tarefas não aparece no mapa: usar
     * {@link TaskTotals#NONE}. A data atual vem do Relógio da organização (RNF06).
     */
    @Transactional(readOnly = true)
    public Map<Long, TaskTotals> totalsByProject(Collection<Long> projectIds) {
        return queries.totalsByProject(projectIds, clock.today());
    }
}
