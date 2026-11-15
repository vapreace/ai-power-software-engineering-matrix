package pt.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Incremento I3 (Ficha PL 03, exercício 5): gestão de tarefas pela API real, com PostgreSQL e a
 * data atual fixa em 2026-11-10 (DA31). Cenários de US05, US07, US10, US11 e US16.
 * <p>
 * "Novo armazém" tem as tarefas T1 a T10 do Background de US11 (5 concluídas). "Armazém Sul"
 * está arquivado e tem uma tarefa aberta da Marta.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class TaskApiTests {

    private static final String PASSWORD = "palavra-passe-de-teste";
    private static final String ADDRESS = "10.9.0.2";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private final Map<String, Long> ids = new HashMap<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        jdbc.update("DELETE FROM task_comments");
        jdbc.update("DELETE FROM task_assignees");
        jdbc.update("DELETE FROM tasks");
        jdbc.update("DELETE FROM projects");
        jdbc.update("DELETE FROM team_members");
        jdbc.update("DELETE FROM teams");
        jdbc.update("DELETE FROM users");

        String hash = passwordEncoder.encode(PASSWORD);
        user("ana", "Ana", "ADMINISTRATOR", hash);
        user("rui", "Rui", "MANAGER", hash);
        user("sofia", "Sofia", "MANAGER", hash);
        user("tiago", "Tiago", "MANAGER", hash);
        user("marta", "Marta", "MEMBER", hash);
        user("pedro", "Pedro", "MEMBER", hash);
        user("carlos", "Carlos", "OBSERVER", hash);

        team("norte", "Logística Norte", "rui", "sofia", "marta", "pedro");
        team("compras", "Compras", "tiago", "marta");

        project("novo", "Novo armazém", "norte", "rui", "ACTIVE");
        project("sul", "Armazém Sul", "norte", "rui", "ARCHIVED");
        project("fornecedores", "Fornecedores", "compras", "tiago", "ACTIVE");

        // Background de US11.
        task("T1", "novo", "TODO", "HIGH", "2026-11-09", "marta");
        task("T2", "novo", "IN_PROGRESS", "MEDIUM", "2026-11-20", "marta", "pedro");
        task("T3", "novo", "DONE", "LOW", "2026-11-05", "marta");
        task("T4", "novo", "DONE", "MEDIUM", "2026-11-08", "marta");
        task("T5", "novo", "IN_PROGRESS", "HIGH", "2026-11-08", "pedro");
        task("T6", "novo", "DONE", "MEDIUM", "2026-11-01", "pedro");
        task("T7", "novo", "TODO", "LOW", "2026-11-15");
        task("T8", "novo", "TODO", "MEDIUM", null);
        task("T9", "novo", "DONE", "MEDIUM", "2026-11-03");
        task("T10", "novo", "DONE", "LOW", "2026-11-02", "pedro");
        task("S1", "sul", "TODO", "MEDIUM", null, "marta");
        task("F1", "fornecedores", "TODO", "MEDIUM", null);
    }

    // ───── US05 Criar tarefa ─────

    @Test
    void managerCreatesATaskThatCanBeReadLater() throws Exception {
        MvcResult created = mvc.perform(createTask(login("rui"), "novo", """
                        {"title": "Pedir orçamento de estantes", "dueDate": "2026-11-15", "priority": "HIGH"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/tasks/\\d+")))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assignees", empty()))
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/tasks/{id}", id).session(login("pedro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pedir orçamento de estantes"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
        mvc.perform(get("/api/projects/{id}", ids.get("novo")).session(login("rui")))
                .andExpect(jsonPath("$.progress.totalTasks").value(11));
    }

    @Test
    void priorityDefaultsToMediumAndTitleIsTrimmed() throws Exception {
        mvc.perform(createTask(login("rui"), "novo", """
                        {"title": "  Rever contrato  "}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Rever contrato"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.dueDate").isEmpty());
    }

    @Test
    void invalidTitlesAreRefused() throws Exception {
        MockHttpSession rui = login("rui");
        for (String title : new String[] {"", "   ", "a".repeat(201), "é".repeat(201)}) {
            mvc.perform(createTask(rui, "novo", "{\"title\": \"%s\"}".formatted(title)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("title"))
                    .andExpect(jsonPath("$.errors[0].message")
                            .value("O título é obrigatório e tem no máximo 200 caracteres."));
        }
        mvc.perform(createTask(rui, "novo", "{\"title\": \"%s\"}".formatted("é".repeat(200))))
                .andExpect(status().isCreated());
        mvc.perform(createTask(rui, "novo", "{\"title\": \"Com descrição\", \"description\": \"%s\"}"
                        .formatted("d".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("description"));
    }

    @Test
    void dueDateCannotBeInThePast() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(createTask(rui, "novo", """
                        {"title": "Inventário", "dueDate": "2026-11-09"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O prazo não pode ser anterior à data atual"))
                .andExpect(jsonPath("$.errors[0].field").value("dueDate"));
        mvc.perform(createTask(rui, "novo", """
                        {"title": "Inventário", "dueDate": "2026-11-10"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void onlyTheManagerOrTheAdministratorCreateTasksAndOnlyInActiveProjects() throws Exception {
        mvc.perform(createTask(login("marta"), "novo", "{\"title\": \"Inventário\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Não tem permissão para esta operação."));
        mvc.perform(createTask(login("sofia"), "novo", "{\"title\": \"Inventário\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(createTask(login("rui"), "sul", "{\"title\": \"Inventário\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_ACTIVE"));
        mvc.perform(createTask(login("ana"), "novo", "{\"title\": \"Inventário\"}"))
                .andExpect(status().isCreated());
    }

    // ───── US07 Mudar estado ─────

    @Test
    void assigneesManagersAndAdministratorsChangeTheStatus() throws Exception {
        mvc.perform(changeStatus(login("marta"), "T1", "IN_PROGRESS", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(changeStatus(login("marta"), "T5", "DONE", 0))
                .andExpect(status().isForbidden());
        mvc.perform(changeStatus(login("sofia"), "T5", "DONE", 0))
                .andExpect(status().isForbidden());
        mvc.perform(changeStatus(login("carlos"), "T5", "DONE", 0))
                .andExpect(status().isForbidden());
        mvc.perform(changeStatus(login("rui"), "T5", "DONE", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
        // Reabrir uma tarefa concluída é permitido e o progresso desce (RN12, E12).
        mvc.perform(changeStatus(login("rui"), "T3", "IN_PROGRESS", 0))
                .andExpect(status().isOk());
        mvc.perform(get("/api/projects/{id}", ids.get("novo")).session(login("rui")))
                .andExpect(jsonPath("$.progress.completedTasks").value(5));
    }

    @Test
    void closedProjectAndOldVersionAreRefused() throws Exception {
        mvc.perform(changeStatus(login("marta"), "S1", "DONE", 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_CLOSED"));

        mvc.perform(changeStatus(login("rui"), "T2", "DONE", 0)).andExpect(status().isOk());
        mvc.perform(changeStatus(login("marta"), "T2", "IN_PROGRESS", 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"))
                .andExpect(jsonPath("$.detail")
                        .value("Esta tarefa foi alterada por outra pessoa. Abra-a de novo e repita a alteração."));
        assertThat(jdbc.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, ids.get("T2")))
                .isEqualTo("DONE");
    }

    // ───── US16 Editar e apagar ─────

    @Test
    void managerEditsAndOldVersionIsRefused() throws Exception {
        mvc.perform(updateTask(login("ana"), "T7", """
                        {"dueDate": "2026-11-20", "version": 0}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(updateTask(login("rui"), "T7", """
                        {"title": "Pedir 3 orçamentos", "version": 0}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
        mvc.perform(updateTask(login("rui"), "T7", """
                        {"title": "Pedir 3 orçamentos", "dueDate": "2026-11-09", "version": 1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("dueDate"));
        mvc.perform(updateTask(login("marta"), "T7", """
                        {"title": "Outro", "version": 1}"""))
                .andExpect(status().isForbidden());
        mvc.perform(updateTask(login("sofia"), "T7", """
                        {"title": "Outro", "version": 1}"""))
                .andExpect(status().isForbidden());
        mvc.perform(updateTask(login("rui"), "T7", """
                        {"status": "DONE", "version": 1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void overdueTaskCanBeEditedWithoutChangingTheDueDateAndTheDueDateCanBeRemoved() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(updateTask(rui, "T1", """
                        {"title": "T1 revista", "dueDate": "2026-11-09", "version": 0}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value("2026-11-09"))
                .andExpect(jsonPath("$.overdue").value(true));
        mvc.perform(updateTask(rui, "T1", """
                        {"dueDate": null, "version": 1}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").isEmpty())
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void closedProjectTasksCannotBeEditedOrDeleted() throws Exception {
        mvc.perform(updateTask(login("ana"), "S1", """
                        {"title": "Outro", "version": 0}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_CLOSED"));
        mvc.perform(delete("/api/tasks/{id}", ids.get("S1")).session(login("ana")).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void deletedTaskDisappearsButIsKept() throws Exception {
        mvc.perform(delete("/api/tasks/{id}", ids.get("T7")).session(login("marta")).with(csrf()))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/tasks/{id}", ids.get("T7")).session(login("rui")).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/projects/{id}", ids.get("novo")).session(login("rui")))
                .andExpect(jsonPath("$.progress.totalTasks").value(9))
                .andExpect(jsonPath("$.progress.percentage").value(55));
        mvc.perform(get("/api/tasks/{id}", ids.get("T7")).session(login("rui")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("A tarefa não existe."));
        mvc.perform(updateTask(login("rui"), "T7", """
                        {"title": "Outro", "version": 0}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("A tarefa não existe."));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks WHERE id = ? AND deleted_at IS NOT NULL",
                Integer.class, ids.get("T7"))).isEqualTo(1);
    }

    // ───── US10, US11 Lista, atrasos e filtros ─────

    @Test
    void listUsesTheOrderOfMyTasksAndMarksOverdueTasks() throws Exception {
        mvc.perform(get("/api/projects/{id}/tasks", ids.get("novo")).session(login("pedro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].title")
                        .value(contains("T6", "T10", "T9", "T3", "T5", "T4", "T1", "T7", "T2", "T8")))
                .andExpect(jsonPath("$.items[4].overdue").value(true))
                .andExpect(jsonPath("$.items[5].overdue").value(false))
                .andExpect(jsonPath("$.items[8].assignees[*].name").value(contains("Marta", "Pedro")))
                .andExpect(jsonPath("$.items[9].overdue").value(false));
    }

    @Test
    void filtersApplyTogether() throws Exception {
        MockHttpSession rui = login("rui");
        String path = "/api/projects/{id}/tasks";
        long novo = ids.get("novo");
        mvc.perform(get(path, novo).param("status", "IN_PROGRESS").session(rui))
                .andExpect(jsonPath("$.items[*].title").value(containsInAnyOrder("T2", "T5")));
        mvc.perform(get(path, novo).param("overdue", "true").session(rui))
                .andExpect(jsonPath("$.items[*].title").value(containsInAnyOrder("T1", "T5")));
        mvc.perform(get(path, novo).param("priority", "HIGH").session(rui))
                .andExpect(jsonPath("$.items[*].title").value(containsInAnyOrder("T1", "T5")));
        mvc.perform(get(path, novo).param("status", "TODO").param("priority", "LOW").session(rui))
                .andExpect(jsonPath("$.items[*].title").value(contains("T7")));
        mvc.perform(get(path, novo).param("status", "DONE").param("overdue", "true").session(rui))
                .andExpect(jsonPath("$.items", empty()));
        mvc.perform(get(path, novo).param("status", "PARADA").session(rui))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void tasksOfOtherTeamsAreNotVisible() throws Exception {
        mvc.perform(get("/api/projects/{id}/tasks", ids.get("fornecedores")).session(login("pedro")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Não tem acesso a esta página."));
        mvc.perform(get("/api/tasks/{id}", ids.get("F1")).session(login("pedro")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/tasks/{id}", ids.get("F1")).session(login("carlos")))
                .andExpect(status().isOk());
    }

    // ───── Dados e pedidos ─────

    private void user(String key, String name, String role, String hash) {
        ids.put(key, jdbc.queryForObject("""
                INSERT INTO users (name, email, password_hash, must_change_password, role, status)
                VALUES (?, ?, ?, false, ?, 'ACTIVE') RETURNING id
                """, Long.class, name, key + "@empresa.example", hash, role));
    }

    private void team(String key, String name, String... members) {
        long team = jdbc.queryForObject("INSERT INTO teams (name) VALUES (?) RETURNING id", Long.class, name);
        ids.put(key, team);
        for (String member : members) {
            jdbc.update("INSERT INTO team_members (team_id, user_id) VALUES (?, ?)", team, ids.get(member));
        }
    }

    private void project(String key, String name, String team, String manager, String status) {
        ids.put(key, jdbc.queryForObject("""
                INSERT INTO projects (team_id, manager_id, name, start_date, status)
                VALUES (?, ?, ?, DATE '2026-10-01', ?) RETURNING id
                """, Long.class, ids.get(team), ids.get(manager), name, status));
    }

    private void task(String title, String project, String status, String priority, String dueDate, String... assignees) {
        long task = jdbc.queryForObject("""
                INSERT INTO tasks (project_id, title, status, priority, due_date)
                VALUES (?, ?, ?, ?, CAST(? AS DATE)) RETURNING id
                """, Long.class, ids.get(project), title, status, priority, dueDate);
        ids.put(title, task);
        for (String assignee : assignees) {
            jdbc.update("INSERT INTO task_assignees (task_id, user_id) VALUES (?, ?)", task, ids.get(assignee));
        }
    }

    private MockHttpSession login(String key) throws Exception {
        MvcResult result = mvc.perform(post("/api/session")
                        .with(csrf())
                        .with(request -> {
                            request.setRemoteAddr(ADDRESS);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s@empresa.example\", \"password\": \"%s\"}".formatted(key, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private RequestBuilder createTask(MockHttpSession session, String project, String body) {
        return post("/api/projects/{id}/tasks", ids.get(project)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private RequestBuilder updateTask(MockHttpSession session, String task, String body) {
        return patch("/api/tasks/{id}", ids.get(task)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private RequestBuilder changeStatus(MockHttpSession session, String task, String status, long version) {
        return put("/api/tasks/{id}/status", ids.get(task)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"%s\", \"version\": %d}".formatted(status, version));
    }
}
