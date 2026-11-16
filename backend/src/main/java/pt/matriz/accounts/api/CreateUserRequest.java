package pt.matriz.accounts.api;

import pt.matriz.common.Role;

record CreateUserRequest(String name, String email, Role role, String temporaryPassword) {
}
