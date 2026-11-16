package pt.matriz.teams.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import pt.matriz.accounts.RoleChanged;
import pt.matriz.common.Role;
import pt.matriz.teams.Teams;

/** Quem passa a Observador ou Administrador deixa de pertencer às equipas (RN23, RN26). */
@Component
class RoleChangedListener {

    private final Teams teams;

    RoleChangedListener(Teams teams) {
        this.teams = teams;
    }

    @EventListener
    void on(RoleChanged event) {
        if (event.newRole() == Role.OBSERVER || event.newRole() == Role.ADMINISTRATOR) {
            teams.removeFromAllTeams(event.accountId());
        }
    }
}
