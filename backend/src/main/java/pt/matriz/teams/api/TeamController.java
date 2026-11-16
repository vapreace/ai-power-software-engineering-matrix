package pt.matriz.teams.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.matriz.common.CurrentUserProvider;
import pt.matriz.common.Items;
import pt.matriz.common.Reference;
import pt.matriz.teams.TeamMember;
import pt.matriz.teams.Teams;

/**
 * Leitura das equipas de openapi.yaml (tag Equipas). No I2 servem o formulário de projeto:
 * a equipa e, para o Administrador, o gestor elegível (US04, RN21, RN27). Criar e gerir
 * equipas (US03) fica para o I7.
 */
@RestController
@RequestMapping("/api/teams")
class TeamController {

    private final Teams teams;
    private final CurrentUserProvider currentUser;

    TeamController(Teams teams, CurrentUserProvider currentUser) {
        this.teams = teams;
        this.currentUser = currentUser;
    }

    @GetMapping
    Items<Reference> list() {
        return new Items<>(teams.visibleTeams(currentUser.get()));
    }

    @PostMapping
    ResponseEntity<Reference> create(@RequestBody TeamRequest body) {
        Reference created = teams.create(currentUser.get(), body.name());
        return ResponseEntity.created(URI.create("/api/teams/" + created.id())).body(created);
    }

    @PatchMapping("/{teamId}")
    Reference rename(@PathVariable("teamId") long teamId, @RequestBody TeamRequest body) {
        return teams.rename(currentUser.get(), teamId, body.name());
    }

    @PutMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void addMember(@PathVariable("teamId") long teamId, @PathVariable("userId") long userId) {
        teams.addMember(currentUser.get(), teamId, userId);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeMember(@PathVariable("teamId") long teamId, @PathVariable("userId") long userId) {
        teams.removeMember(currentUser.get(), teamId, userId);
    }

    @GetMapping("/{teamId}/members")
    Items<TeamMember> members(@PathVariable("teamId") long teamId) {
        List<TeamMember> members = teams.members(currentUser.get(), teamId);
        return new Items<>(members);
    }
}
