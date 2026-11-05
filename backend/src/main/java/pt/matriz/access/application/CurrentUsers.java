package pt.matriz.access.application;

import org.springframework.stereotype.Component;

import pt.matriz.accounts.AccountView;
import pt.matriz.common.CurrentUser;
import pt.matriz.teams.Teams;

/** Monta o utilizador atual a partir da conta e das equipas (architecture.md, secção 5.2). */
@Component
public class CurrentUsers {

    /** Atributo do pedido onde fica o utilizador atual, depois de verificada a sessão. */
    public static final String REQUEST_ATTRIBUTE = CurrentUser.class.getName();

    private final Teams teams;

    CurrentUsers(Teams teams) {
        this.teams = teams;
    }

    public CurrentUser from(AccountView account) {
        return new CurrentUser(account.id(), account.name(), account.email(), account.role(),
                teams.teamsOf(account.id()), account.mustChangePassword());
    }
}
