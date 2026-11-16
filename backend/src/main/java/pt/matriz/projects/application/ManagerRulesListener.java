package pt.matriz.projects.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import pt.matriz.accounts.AccountDeactivated;
import pt.matriz.accounts.RoleChanged;
import pt.matriz.common.Role;
import pt.matriz.projects.Projects;
import pt.matriz.teams.TeamMemberRemoved;

/**
 * O projeto fica "Sem gestor" quando o gestor deixa de cumprir RN21 (RN22, architecture.md secção 7):
 * conta desativada, papel mudado ou saída da equipa do projeto.
 */
@Component
class ManagerRulesListener {

    private final Projects projects;

    ManagerRulesListener(Projects projects) {
        this.projects = projects;
    }

    @EventListener
    void on(AccountDeactivated event) {
        projects.clearManager(event.accountId(), null);
    }

    @EventListener
    void on(RoleChanged event) {
        if (event.previousRole() == Role.MANAGER && event.newRole() != Role.MANAGER) {
            projects.clearManager(event.accountId(), null);
        }
    }

    @EventListener
    void on(TeamMemberRemoved event) {
        projects.clearManager(event.accountId(), event.teamId());
    }
}
