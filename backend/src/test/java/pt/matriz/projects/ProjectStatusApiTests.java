package pt.matriz.projects;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.RequestBuilder;

import pt.matriz.ApiTestSupport;
import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Incremento I7 (resto do MVP): estado do projeto (US13) e mudança de gestor (US17), pela API real.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class ProjectStatusApiTests extends ApiTestSupport {

    ProjectStatusApiTests() {
        super("10.9.0.7");
    }

    @BeforeEach
    void setUp() {
        user("ana", "Ana", "ADMINISTRATOR");
        user("rui", "Rui", "MANAGER");
        user("sofia", "Sofia", "MANAGER");
        user("tiago", "Tiago", "MANAGER");
        user("marta", "Marta", "MEMBER");
        user("pedro", "Pedro", "MEMBER");

        team("norte", "Logística Norte", "rui", "sofia", "marta", "pedro");
        team("compras", "Compras", "tiago");

        project("novo", "Novo armazém", "norte", "rui", "ACTIVE");
        project("sul", "Armazém Sul", "norte", "rui", "ARCHIVED");
        project("frota", "Frota", "norte", "sofia", "ACTIVE");

        task("N1", "novo", "TODO", "MEDIUM", null, "marta");
        task("S1", "sul", "TODO", "MEDIUM", null, "marta");
    }

    // ───── US13 Mudar estado e arquivar projeto ─────

    @Test
    void managerCompletesReopensArchivesAndUnarchives() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(changeProjectStatus(rui, "novo", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(createTask(rui, "novo")).andExpect(status().isConflict());

        mvc.perform(changeProjectStatus(rui, "novo", "ACTIVE")).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(createTask(rui, "novo")).andExpect(status().isCreated());

        mvc.perform(changeProjectStatus(rui, "novo", "COMPLETED")).andExpect(status().isOk());
        mvc.perform(changeProjectStatus(rui, "novo", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
        mvc.perform(get("/api/projects").session(rui))
                .andExpect(jsonPath("$.items[*].name").value(not(hasItem("Novo armazém"))));

        mvc.perform(changeProjectStatus(rui, "sul", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void transitionsOutsideTheDiagramAreRefused() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(changeProjectStatus(rui, "novo", "ARCHIVED"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"))
                .andExpect(jsonPath("$.detail").value("Esta mudança de estado não é permitida."));
        mvc.perform(changeProjectStatus(rui, "novo", "ACTIVE")).andExpect(status().isConflict());
        mvc.perform(changeProjectStatus(rui, "sul", "ACTIVE")).andExpect(status().isConflict());
    }

    @Test
    void archivedProjectIsReadOnly() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(patch("/api/projects/{id}", ids.get("sul")).session(rui).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Outro\", \"version\": %d}".formatted(projectVersion("sul"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_CLOSED"));
        mvc.perform(createTask(rui, "sul")).andExpect(status().isConflict());
        mvc.perform(patch("/api/tasks/{id}", ids.get("S1")).session(login("ana")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Outro\", \"version\": %d}".formatted(taskVersion("S1"))))
                .andExpect(status().isConflict());
        mvc.perform(changeStatus(login("marta"), "S1", "DONE", taskVersion("S1"))).andExpect(status().isConflict());
    }

    @Test
    void onlyTheAdministratorOrTheProjectManagerChangeTheStatus() throws Exception {
        mvc.perform(changeProjectStatus(login("marta"), "novo", "COMPLETED")).andExpect(status().isForbidden());
        mvc.perform(changeProjectStatus(login("sofia"), "novo", "COMPLETED")).andExpect(status().isForbidden());

        jdbc.update("UPDATE projects SET manager_id = NULL WHERE id = ?", ids.get("novo"));
        mvc.perform(changeProjectStatus(login("rui"), "novo", "COMPLETED")).andExpect(status().isForbidden());
        mvc.perform(changeProjectStatus(login("ana"), "novo", "COMPLETED")).andExpect(status().isOk());
    }

    @Test
    void statusRequestsAreValidatedAndUseTheVersion() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(put("/api/projects/{id}/status", ids.get("novo")).session(rui).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": %d}".formatted(projectVersion("novo"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
        long old = projectVersion("novo");
        mvc.perform(changeProjectStatus(rui, "novo", "COMPLETED")).andExpect(status().isOk());
        mvc.perform(put("/api/projects/{id}/status", ids.get("novo")).session(rui).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\", \"version\": %d}".formatted(old)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        mvc.perform(put("/api/projects/{id}/status", 999_999).session(rui).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"ACTIVE\", \"version\": 0}"))
                .andExpect(status().isNotFound());
    }

    // ───── US17 Mudar o gestor ─────

    @Test
    void administratorChangesTheManagerAlsoInAClosedProject() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(changeManager(ana, "novo", "sofia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manager.name").value("Sofia"));
        mvc.perform(changeManager(ana, "sul", "sofia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manager.name").value("Sofia"))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void managerCannotChangeTheManager() throws Exception {
        mvc.perform(changeManager(login("rui"), "novo", "sofia")).andExpect(status().isForbidden());
    }

    @Test
    void invalidManagersAreRefused() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(changeManager(ana, "novo", "tiago"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_MANAGER"));
        mvc.perform(changeManager(ana, "novo", "pedro")).andExpect(status().isConflict());
        jdbc.update("UPDATE users SET status = 'DEACTIVATED' WHERE id = ?", ids.get("sofia"));
        mvc.perform(changeManager(ana, "novo", "sofia")).andExpect(status().isConflict());
        mvc.perform(put("/api/projects/{id}/manager", ids.get("novo")).session(ana).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": %d}".formatted(projectVersion("novo"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("managerId"));
    }

    @Test
    void newManagerEditsAProjectThatWasWithoutManager() throws Exception {
        jdbc.update("UPDATE projects SET manager_id = NULL WHERE id = ?", ids.get("novo"));
        MockHttpSession sofia = login("sofia");
        mvc.perform(renameProject(sofia, "novo", "Armazém Norte")).andExpect(status().isForbidden());

        mvc.perform(changeManager(login("ana"), "novo", "sofia")).andExpect(status().isOk());
        mvc.perform(renameProject(sofia, "novo", "Armazém Norte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Armazém Norte"));
    }

    private long projectVersion(String project) {
        return jdbc.queryForObject("SELECT version FROM projects WHERE id = ?", Long.class, ids.get(project));
    }

    private long taskVersion(String task) {
        return jdbc.queryForObject("SELECT version FROM tasks WHERE id = ?", Long.class, ids.get(task));
    }

    private RequestBuilder changeProjectStatus(MockHttpSession session, String project, String status) {
        return put("/api/projects/{id}/status", ids.get(project)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"%s\", \"version\": %d}".formatted(status, projectVersion(project)));
    }

    private RequestBuilder changeManager(MockHttpSession session, String project, String person) {
        return put("/api/projects/{id}/manager", ids.get(project)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"managerId\": %d, \"version\": %d}".formatted(ids.get(person), projectVersion(project)));
    }

    private RequestBuilder renameProject(MockHttpSession session, String project, String name) {
        return patch("/api/projects/{id}", ids.get(project)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"%s\", \"version\": %d}".formatted(name, projectVersion(project)));
    }

    private RequestBuilder createTask(MockHttpSession session, String project) {
        return post("/api/projects/{id}/tasks", ids.get(project)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Tarefa nova\"}");
    }
}
