package pt.matriz.tracking.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.matriz.common.CurrentUserProvider;
import pt.matriz.tracking.application.Dashboard;
import pt.matriz.tracking.application.ProjectTracking;

/** Painel inicial de openapi.yaml (tag Painel, US21). O utilizador autenticado vem de {@code GET /session}. */
@RestController
class DashboardController {

    private final ProjectTracking tracking;
    private final CurrentUserProvider currentUser;

    DashboardController(ProjectTracking tracking, CurrentUserProvider currentUser) {
        this.tracking = tracking;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/dashboard")
    Dashboard dashboard() {
        return tracking.dashboard(currentUser.get());
    }
}
