package pt.matriz.tasks;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

import pt.matriz.ApiTestSupport;
import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/** Incremento I7 (resto do MVP): "As minhas tarefas" pela API real. Cenários de US08 (RN24, RN25). */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class MyTasksApiTests extends ApiTestSupport {

    MyTasksApiTests() {
        super("10.9.0.8");
    }

    @BeforeEach
    void setUp() {
        user("rui", "Rui", "MANAGER");
        user("tiago", "Tiago", "MANAGER");
        user("marta", "Marta", "MEMBER");
        user("pedro", "Pedro", "MEMBER");
        user("carlos", "Carlos", "OBSERVER");

        team("norte", "Logística Norte", "rui", "marta", "pedro");
        team("compras", "Compras", "tiago", "marta");

        project("novo", "Novo armazém", "norte", "rui", "ACTIVE");
        project("sul", "Armazém Sul", "norte", "rui", "ARCHIVED");
        project("fornecedores", "Fornecedores", "compras", "tiago", "ACTIVE");
    }

    private void background() {
        task("N1", "novo", "TODO", "MEDIUM", "2026-11-20", "marta");
        task("N2", "novo", "IN_PROGRESS", "HIGH", "2026-11-09", "marta", "pedro");
        task("N3", "novo", "TODO", "LOW", null, "marta");
        task("N4", "novo", "DONE", "MEDIUM", "2026-11-01", "marta");
        task("F1", "fornecedores", "TODO", "MEDIUM", "2026-11-15", "marta");
        task("F2", "fornecedores", "IN_PROGRESS", "LOW", "2026-11-16", "marta");
        task("S1", "sul", "TODO", "MEDIUM", null, "marta");
        task("P1", "novo", "TODO", "MEDIUM", null, "pedro");
    }

    @Test
    void showsTheOpenTasksOfActiveProjectsWithTheProject() throws Exception {
        background();
        MockHttpSession marta = login("marta");
        mvc.perform(get("/api/me/tasks").session(marta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.items[0].title").value("N2"))
                .andExpect(jsonPath("$.items[0].project.name").value("Novo armazém"))
                .andExpect(jsonPath("$.items[0].overdue").value(true))
                .andExpect(jsonPath("$.items[0].assignees[*].name").value(contains("Marta", "Pedro")));
        mvc.perform(get("/api/me/tasks").param("includeCompleted", "true").session(marta))
                .andExpect(jsonPath("$.items", hasSize(6)));
    }

    @Test
    void completedProjectsAreLeftOut() throws Exception {
        background();
        jdbc.update("UPDATE projects SET status = 'COMPLETED' WHERE id = ?", ids.get("fornecedores"));
        mvc.perform(get("/api/me/tasks").session(login("marta")))
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    @Test
    void orderIsDueDateThenPriorityThenTitle() throws Exception {
        task("Inventário", "novo", "TODO", "LOW", "2026-11-20", "marta");
        task("Orçamento", "novo", "TODO", "MEDIUM", "2026-11-12", "marta");
        task("Transporte", "novo", "TODO", "HIGH", "2026-11-12", "marta");
        task("Arquivo", "novo", "TODO", "HIGH", null, "marta");
        task("Contrato", "novo", "TODO", "HIGH", "2026-11-12", "marta");
        mvc.perform(get("/api/me/tasks").session(login("marta")))
                .andExpect(jsonPath("$.items[*].title")
                        .value(contains("Contrato", "Transporte", "Orçamento", "Inventário", "Arquivo")));
    }

    @Test
    void titlesAreOrderedWithoutCaseOrAccents() throws Exception {
        task("ótica", "novo", "TODO", "MEDIUM", "2026-11-12", "marta");
        task("Obras", "novo", "TODO", "MEDIUM", "2026-11-12", "marta");
        task("orçamento", "novo", "TODO", "MEDIUM", "2026-11-12", "marta");
        mvc.perform(get("/api/me/tasks").session(login("marta")))
                .andExpect(jsonPath("$.items[*].title").value(contains("Obras", "orçamento", "ótica")));
    }

    @Test
    void removedAssignmentsAndDeletedTasksAreLeftOut() throws Exception {
        background();
        jdbc.update("UPDATE task_assignees SET removed_at = now() WHERE task_id = ? AND user_id = ?",
                ids.get("N1"), ids.get("marta"));
        jdbc.update("UPDATE tasks SET deleted_at = now() WHERE id = ?", ids.get("N3"));
        mvc.perform(get("/api/me/tasks").session(login("marta")))
                .andExpect(jsonPath("$.items[*].title").value(contains("N2", "F1", "F2")));
    }

    @Test
    void personWithoutTasksGetsAnEmptyList() throws Exception {
        background();
        mvc.perform(get("/api/me/tasks").session(login("carlos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(empty()));
    }
}
