package pt.matriz.tasks.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import pt.matriz.accounts.AccountDeactivated;
import pt.matriz.teams.TeamMemberRemoved;

/**
 * Responsáveis retirados automaticamente (architecture.md secção 7): ao desativar a conta, de todas
 * as tarefas abertas (RN16); ao sair de uma equipa, das tarefas abertas dos projetos dessa equipa (RN17).
 */
@Component
class AssigneeRulesListener {

    private final TaskService tasks;

    AssigneeRulesListener(TaskService tasks) {
        this.tasks = tasks;
    }

    @EventListener
    void on(AccountDeactivated event) {
        tasks.removeFromOpenTasks(event.accountId(), null);
    }

    @EventListener
    void on(TeamMemberRemoved event) {
        tasks.removeFromOpenTasks(event.accountId(), event.teamId());
    }
}
