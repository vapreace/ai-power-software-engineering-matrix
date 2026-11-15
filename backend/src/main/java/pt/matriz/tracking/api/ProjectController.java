package pt.matriz.tracking.api;

import java.net.URI;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.matriz.common.CurrentUserProvider;
import pt.matriz.common.Items;
import pt.matriz.projects.ProjectStatus;
import pt.matriz.tracking.application.ProjectDetail;
import pt.matriz.tracking.application.ProjectSummary;
import pt.matriz.tracking.application.ProjectTracking;

/**
 * Operações de projeto do I2 em openapi.yaml (tag Projetos): listar e pesquisar, criar,
 * consultar, editar e eliminar (US04, US09, US10, US12, US15, US17, US20, US22).
 * Mudar o estado e o gestor ficam para o I7.
 */
@RestController
@RequestMapping("/api/projects")
class ProjectController {

    private final ProjectTracking tracking;
    private final CurrentUserProvider currentUser;

    ProjectController(ProjectTracking tracking, CurrentUserProvider currentUser) {
        this.tracking = tracking;
        this.currentUser = currentUser;
    }

    @GetMapping
    Items<ProjectSummary> list(@RequestParam(name = "status", required = false) Set<ProjectStatus> status,
            @RequestParam(name = "q", required = false) String q) {
        return new Items<>(tracking.list(currentUser.get(), status, q));
    }

    @PostMapping
    ResponseEntity<ProjectDetail> create(@RequestBody CreateProjectRequest body) {
        ProjectDetail created = tracking.create(currentUser.get(), body.toCommand());
        return ResponseEntity.created(URI.create("/api/projects/" + created.id())).body(created);
    }

    @GetMapping("/{projectId}")
    ProjectDetail get(@PathVariable("projectId") long projectId) {
        return tracking.get(currentUser.get(), projectId);
    }

    @PatchMapping("/{projectId}")
    ProjectDetail update(@PathVariable("projectId") long projectId, @RequestBody UpdateProjectRequest body) {
        return tracking.update(currentUser.get(), projectId, body.toCommand());
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("projectId") long projectId) {
        tracking.delete(currentUser.get(), projectId);
    }
}
