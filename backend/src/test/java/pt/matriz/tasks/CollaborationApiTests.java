package pt.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

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
 * Incremento I4 (Ficha PL 03, exercício 6): responsáveis, filtro por responsável e comentários,
 * pela API real, com PostgreSQL e a data e hora fixas em 2026-11-10 10:00 (DA31). Cenários de
 * US06, US07 (com responsáveis), US11 e US19.
 * <p>
 * "Novo armazém" tem as tarefas T1 a T10 do Background de US11. "Frota" (gestora Sofia) tem uma
 * tarefa do Rui. "Armazém Sul" está arquivado. O João pertence só a "Compras".
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class CollaborationApiTests {

    private static final String PASSWORD = "palavra-passe-de-teste";
    private static final String ADDRESS = "10.9.0.3";

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
        user("joao", "João", "MEMBER", hash);
        user("carlos", "Carlos", "OBSERVER", hash);

        team("norte", "Logística Norte", "rui", "sofia", "marta", "pedro");
        team("compras", "Compras", "tiago", "marta", "joao");

        project("novo", "Novo armazém", "norte", "rui", "ACTIVE");
        project("frota", "Frota", "norte", "sofia", "ACTIVE");
        project("sul", "Armazém Sul", "norte", "rui", "ARCHIVED");

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
        task("FR1", "frota", "TODO", "MEDIUM", null, "rui");
        task("S1", "sul", "TODO", "MEDIUM", null, "marta");
    }

    // ───── US06 Gerir responsáveis ─────

    @Test
    void managerAssignsSeveralPeopleAndTheVersionIncreases() throws Exception {
        mvc.perform(assign(login("rui"), "T7", "marta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignees[*].name").value(contains("Marta")))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(assign(login("rui"), "T7", "pedro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignees[*].name").value(contains("Marta", "Pedro")))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void theSamePersonIsNotAssignedTwice() throws Exception {
        mvc.perform(assign(login("rui"), "T1", "marta"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ASSIGNEE"))
                .andExpect(jsonPath("$.detail").value("Esta pessoa já é responsável pela tarefa."));
        mvc.perform(get("/api/tasks/{id}", ids.get("T1")).session(login("rui")))
                .andExpect(jsonPath("$.assignees", hasSize(1)));
    }

    @Test
    void invalidAssigneesAreRefused() throws Exception {
        mvc.perform(assign(login("rui"), "T7", "joao"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNEE"));
        jdbc.update("UPDATE users SET status = 'DEACTIVATED' WHERE id = ?", ids.get("pedro"));
        mvc.perform(assign(login("rui"), "T7", "pedro"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNEE"));
        mvc.perform(assign(login("rui"), "T7", "carlos"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/tasks/{id}/assignees/{userId}", ids.get("T7"), 999_999)
                        .session(login("rui")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyTheManagerOrTheAdministratorManageAssignees() throws Exception {
        mvc.perform(assign(login("marta"), "T7", "pedro")).andExpect(status().isForbidden());
        mvc.perform(assign(login("sofia"), "T7", "pedro")).andExpect(status().isForbidden());
        mvc.perform(assign(login("ana"), "T7", "pedro")).andExpect(status().isOk());
        mvc.perform(assign(login("ana"), "S1", "pedro"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_CLOSED"));
    }

    @Test
    void removingOneOfSeveralKeepsTheOthersAndRemovingTheLastLeavesNoAssignee() throws Exception {
        mvc.perform(unassign(login("rui"), "T2", "marta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignees[*].name").value(contains("Pedro")))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(unassign(login("rui"), "T1", "marta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignees", hasSize(0)));
        mvc.perform(unassign(login("rui"), "T1", "marta"))
                .andExpect(status().isNotFound());
        // A associação fica guardada, marcada como removida (RN32).
        assertThat(jdbc.queryForObject("SELECT count(*) FROM task_assignees WHERE task_id = ? AND removed_at IS NOT NULL",
                Integer.class, ids.get("T1"))).isEqualTo(1);
        // Associar de novo cria uma linha nova.
        mvc.perform(assign(login("rui"), "T1", "marta")).andExpect(status().isOk());
    }

    // ───── US07 com responsáveis ─────

    @Test
    void managerWhoIsAssigneeInAProjectTheyDoNotManageChangesTheStatus() throws Exception {
        mvc.perform(changeStatus(login("rui"), "FR1", "IN_PROGRESS", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void assigneeKeepsChangingTheStatusInAProjectWithoutManager() throws Exception {
        jdbc.update("UPDATE projects SET manager_id = NULL WHERE id = ?", ids.get("novo"));
        mvc.perform(changeStatus(login("marta"), "T1", "DONE", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
        mvc.perform(changeStatus(login("rui"), "T7", "DONE", 0))
                .andExpect(status().isForbidden());
    }

    // ───── US11 Filtro por responsável ─────

    @Test
    void assigneeFilterCombinesWithTheOthers() throws Exception {
        MockHttpSession rui = login("rui");
        String path = "/api/projects/{id}/tasks";
        long novo = ids.get("novo");
        mvc.perform(get(path, novo).param("assigneeId", ids.get("marta").toString()).param("overdue", "true").session(rui))
                .andExpect(jsonPath("$.items[*].title").value(contains("T1")));
        mvc.perform(get(path, novo).param("unassigned", "true").session(rui))
                .andExpect(jsonPath("$.items[*].title").value(containsInAnyOrder("T7", "T8", "T9")));
        mvc.perform(get(path, novo).param("assigneeId", ids.get("pedro").toString()).param("status", "IN_PROGRESS").session(rui))
                .andExpect(jsonPath("$.items[*].title").value(containsInAnyOrder("T2", "T5")));
        mvc.perform(get(path, novo).param("assigneeId", ids.get("pedro").toString()).param("status", "TODO").session(rui))
                .andExpect(jsonPath("$.items", empty()));
        mvc.perform(get(path, novo).param("assigneeId", ids.get("pedro").toString()).param("unassigned", "true").session(rui))
                .andExpect(status().isBadRequest());
    }

    // ───── US19 Comentar tarefa ─────

    @Test
    void commentHasTheAuthenticatedAuthorAndTheOrganizationTime() throws Exception {
        mvc.perform(comment(login("marta"), "T1", """
                        {"content": "Já pedi dois orçamentos.", "authorId": %d, "createdAt": "2020-01-01T00:00:00Z"}"""
                        .formatted(ids.get("pedro"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.name").value("Marta"))
                .andExpect(jsonPath("$.content").value("Já pedi dois orçamentos."))
                .andExpect(jsonPath("$.createdAt").value(matchesPattern("2026-11-10T10:00(:00)?(\\.0+)?(Z|\\+00:00)")));
        // Comentar não muda a versão da tarefa (DA20).
        mvc.perform(get("/api/tasks/{id}", ids.get("T1")).session(login("rui")))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void commentsAreKeptInOrderAndTheObserverReadsThem() throws Exception {
        jdbc.update("""
                INSERT INTO task_comments (task_id, author_id, content, created_at)
                VALUES (?, ?, 'Preciso disto até sexta.', TIMESTAMPTZ '2026-11-09 10:00:00+00')
                """, ids.get("T2"), ids.get("rui"));
        mvc.perform(comment(login("marta"), "T2", "{\"content\": \"Já pedi dois orçamentos.\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/tasks/{id}/comments", ids.get("T2")).session(login("carlos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].author.name").value(contains("Rui", "Marta")));
        mvc.perform(comment(login("carlos"), "T2", "{\"content\": \"Visto.\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(comment(login("joao"), "T2", "{\"content\": \"Olá.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void commentTextIsValidatedAndShownExactly() throws Exception {
        MockHttpSession marta = login("marta");
        for (String text : new String[] {"", "   ", "c".repeat(2001)}) {
            mvc.perform(comment(marta, "T1", "{\"content\": \"%s\"}".formatted(text)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("content"));
        }
        mvc.perform(comment(marta, "T1", "{\"content\": \"%s\"}".formatted("c".repeat(2000))))
                .andExpect(status().isCreated());
        mvc.perform(comment(marta, "T1", "{\"content\": \"<b>urgente</b>\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("<b>urgente</b>"));
    }

    @Test
    void closedProjectsDoNotAcceptComments() throws Exception {
        mvc.perform(comment(login("marta"), "S1", "{\"content\": \"Olá.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_CLOSED"));
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

    private RequestBuilder assign(MockHttpSession session, String task, String person) {
        return put("/api/tasks/{id}/assignees/{userId}", ids.get(task), ids.get(person)).session(session).with(csrf());
    }

    private RequestBuilder unassign(MockHttpSession session, String task, String person) {
        return delete("/api/tasks/{id}/assignees/{userId}", ids.get(task), ids.get(person)).session(session).with(csrf());
    }

    private RequestBuilder comment(MockHttpSession session, String task, String body) {
        return post("/api/tasks/{id}/comments", ids.get(task)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private RequestBuilder changeStatus(MockHttpSession session, String task, String status, long version) {
        return put("/api/tasks/{id}/status", ids.get(task)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"%s\", \"version\": %d}".formatted(status, version));
    }
}
