package pt.matriz.accounts.api;

import pt.matriz.accounts.AccountStatus;

record ChangeUserStatusRequest(AccountStatus status) {
}
