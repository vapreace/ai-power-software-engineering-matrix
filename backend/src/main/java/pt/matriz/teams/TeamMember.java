package pt.matriz.teams;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.common.Role;

/** Pessoa que pertence a uma equipa, com o papel e o estado da conta (TeamMember em openapi.yaml). */
public record TeamMember(long id, String name, Role role, AccountStatus status) {
}
