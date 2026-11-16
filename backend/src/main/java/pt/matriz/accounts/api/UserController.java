package pt.matriz.accounts.api;

import java.net.URI;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.application.AccountAdministration;
import pt.matriz.common.CurrentUserProvider;
import pt.matriz.common.Items;
import pt.matriz.common.Role;

/** Contas (US02, US14, US18). Só o Administrador; as regras estão em {@link AccountAdministration}. */
@RestController
@RequestMapping("/api/users")
class UserController {

    private final AccountAdministration administration;
    private final CurrentUserProvider currentUser;

    UserController(AccountAdministration administration, CurrentUserProvider currentUser) {
        this.administration = administration;
        this.currentUser = currentUser;
    }

    @GetMapping
    Items<AccountView> list(@RequestParam(name = "status", required = false) Set<AccountStatus> status,
            @RequestParam(name = "role", required = false) Set<Role> role) {
        return new Items<>(administration.list(currentUser.get(), status, role));
    }

    @PostMapping
    ResponseEntity<AccountView> create(@RequestBody CreateUserRequest body) {
        AccountView created = administration.create(currentUser.get(), body.name(), body.email(), body.role(),
                body.temporaryPassword());
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @PatchMapping("/{userId}")
    AccountView update(@PathVariable("userId") long userId, @RequestBody UpdateUserRequest body) {
        return administration.update(currentUser.get(), userId, body.name(), body.email(), body.role(),
                body.unknownFields());
    }

    @PutMapping("/{userId}/status")
    AccountView changeStatus(@PathVariable("userId") long userId, @RequestBody ChangeUserStatusRequest body) {
        return administration.changeStatus(currentUser.get(), userId, body.status());
    }

    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@PathVariable("userId") long userId, @RequestBody ResetPasswordRequest body) {
        administration.resetPassword(currentUser.get(), userId, body.temporaryPassword());
    }
}
