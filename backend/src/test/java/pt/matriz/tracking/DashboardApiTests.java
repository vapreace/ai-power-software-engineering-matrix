package pt.matriz.tracking;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Incremento I5 (Ficha PL 03, exercício 7): painel inicial pela API real (US21, RN33), com a
 * tabela do Background de US21: "Novo armazém" 2/2/6, "Frota" 1/0/0, "Armazém Sul" (arquivado)
 * 0/0/4 e "Fornecedores" 3/1/1 (Por fazer / Em curso / Concluída).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class DashboardApiTests {

    private static final String PASSWORD = "palavra-passe-de-teste";
    private static final String ADDRESS = "10.9.0.4";

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
        user("rui", "Rui", "MANAGER", hash);
        user("sofia", "Sofia", "MANAGER", hash);
        user("tiago", "Tiago", "MANAGER", hash);
        user("carlos", "Carlos", "OBSERVER", hash);

        team("norte", "Logística Norte", "rui", "sofia");
        team("compras", "Compras", "tiago");

        project("novo", "Novo armazém", "norte", "rui", "ACTIVE", 2, 2, 6);
        project("frota", "Frota", "norte", "sofia", "ACTIVE", 1, 0, 0);
        project("sul", "Armazém Sul", "norte", "rui", "ARCHIVED", 0, 0, 4);
        project("fornecedores", "Fornecedores", "compras", "tiago", "ACTIVE", 3, 1, 1);
    }

    @Test
    void managerSeesTheProjectsOfTheirTeamsWithCountsAndTotals() throws Exception {
        mvc.perform(get("/api/dashboard").session(login("rui")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[*].name").value(contains("Frota", "Novo armazém")))
                .andExpect(jsonPath("$.projects[0].taskCounts.todo").value(1))
                .andExpect(jsonPath("$.projects[0].manager.name").value("Sofia"))
                .andExpect(jsonPath("$.projects[1].taskCounts.todo").value(2))
                .andExpect(jsonPath("$.projects[1].taskCounts.inProgress").value(2))
                .andExpect(jsonPath("$.projects[1].taskCounts.done").value(6))
                .andExpect(jsonPath("$.projects[1].progress.percentage").value(60))
                .andExpect(jsonPath("$.totals.todo").value(3))
                .andExpect(jsonPath("$.totals.inProgress").value(2))
                .andExpect(jsonPath("$.totals.done").value(6));
    }

    @Test
    void observerSeesAllTeams() throws Exception {
        mvc.perform(get("/api/dashboard").session(login("carlos")))
                .andExpect(jsonPath("$.projects[*].name").value(contains("Fornecedores", "Frota", "Novo armazém")))
                .andExpect(jsonPath("$.totals.todo").value(6))
                .andExpect(jsonPath("$.totals.inProgress").value(3))
                .andExpect(jsonPath("$.totals.done").value(7));
    }

    @Test
    void countsChangeAfterCreatingOrDeletingTasks() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(post("/api/projects/{id}/tasks", ids.get("novo")).session(rui).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Contratar seguro\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/dashboard").session(rui))
                .andExpect(jsonPath("$.projects[1].taskCounts.todo").value(3))
                .andExpect(jsonPath("$.totals.todo").value(4));

        mvc.perform(delete("/api/projects/{id}", ids.get("frota")).session(login("sofia")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/dashboard").session(rui))
                .andExpect(jsonPath("$.projects[*].name").value(contains("Novo armazém")))
                .andExpect(jsonPath("$.totals.todo").value(3));
    }

    @Test
    void managerWithoutTeamSeesNoProjects() throws Exception {
        jdbc.update("UPDATE team_members SET removed_at = now() WHERE user_id = ?", ids.get("tiago"));
        mvc.perform(get("/api/dashboard").session(login("tiago")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects", empty()))
                .andExpect(jsonPath("$.totals.todo").value(0));
    }

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

    private void project(String key, String name, String team, String manager, String status,
            int todo, int inProgress, int done) {
        long project = jdbc.queryForObject("""
                INSERT INTO projects (team_id, manager_id, name, start_date, status)
                VALUES (?, ?, ?, DATE '2026-10-01', ?) RETURNING id
                """, Long.class, ids.get(team), ids.get(manager), name, status);
        ids.put(key, project);
        tasks(project, "TODO", todo);
        tasks(project, "IN_PROGRESS", inProgress);
        tasks(project, "DONE", done);
    }

    private void tasks(long project, String status, int count) {
        for (int i = 1; i <= count; i++) {
            jdbc.update("INSERT INTO tasks (project_id, title, status) VALUES (?, ?, ?)",
                    project, status + " " + i, status);
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
}
