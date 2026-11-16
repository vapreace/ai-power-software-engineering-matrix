package pt.matriz;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base comum dos testes de API (refatoração do I6, docs/verification.md secção 10).
 * <p>
 * Antes de cada teste limpa a base de dados e prepara o MockMvc com o Spring Security. Os dados
 * de cada teste são criados pela subclasse com {@link #user}, {@link #team}, {@link #project} e
 * {@link #task}, e ficam em {@link #ids} com a chave dada (por exemplo "rui" ou "novo").
 * Todas as pessoas têm a mesma palavra-passe de teste e o email {@code <chave>@empresa.example}.
 * <p>
 * Cada classe usa um endereço IP próprio para entrar, para o limite de tentativas (DA26) de uma
 * classe não afetar as outras.
 */
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "palavra-passe-de-teste";

    private final String address;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected MockMvc mvc;
    protected final Map<String, Long> ids = new HashMap<>();
    private String hash;

    protected ApiTestSupport(String address) {
        this.address = address;
    }

    @BeforeEach
    protected void prepareMvcAndCleanDatabase() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        jdbc.update("DELETE FROM task_comments");
        jdbc.update("DELETE FROM task_assignees");
        jdbc.update("DELETE FROM tasks");
        jdbc.update("DELETE FROM projects");
        jdbc.update("DELETE FROM team_members");
        jdbc.update("DELETE FROM teams");
        jdbc.update("DELETE FROM users");

        ids.clear();
        hash = passwordEncoder.encode(PASSWORD);
    }

    protected void user(String key, String name, String role) {
        ids.put(key, jdbc.queryForObject("""
                INSERT INTO users (name, email, password_hash, must_change_password, role, status)
                VALUES (?, ?, ?, false, ?, 'ACTIVE') RETURNING id
                """, Long.class, name, key + "@empresa.example", hash, role));
    }

    protected void team(String key, String name, String... members) {
        long team = jdbc.queryForObject("INSERT INTO teams (name) VALUES (?) RETURNING id", Long.class, name);
        ids.put(key, team);
        for (String member : members) {
            jdbc.update("INSERT INTO team_members (team_id, user_id) VALUES (?, ?)", team, ids.get(member));
        }
    }

    protected void project(String key, String name, String team, String manager, String status) {
        ids.put(key, jdbc.queryForObject("""
                INSERT INTO projects (team_id, manager_id, name, start_date, status)
                VALUES (?, ?, ?, DATE '2026-10-01', ?) RETURNING id
                """, Long.class, ids.get(team), ids.get(manager), name, status));
    }

    /** Cria a tarefa com a chave {@code title} e associa os responsáveis indicados. */
    protected void task(String title, String project, String status, String priority, String dueDate,
            String... assignees) {
        long task = jdbc.queryForObject("""
                INSERT INTO tasks (project_id, title, status, priority, due_date)
                VALUES (?, ?, ?, ?, CAST(? AS DATE)) RETURNING id
                """, Long.class, ids.get(project), title, status, priority, dueDate);
        ids.put(title, task);
        for (String assignee : assignees) {
            jdbc.update("INSERT INTO task_assignees (task_id, user_id) VALUES (?, ?)", task, ids.get(assignee));
        }
    }

    /** Entra pela API com a pessoa {@code key} e devolve a sessão. */
    protected MockHttpSession login(String key) throws Exception {
        MvcResult result = attemptLogin(key + "@empresa.example", PASSWORD)
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /** Tentativa de entrada com qualquer email e palavra-passe, a partir do endereço da classe. */
    protected ResultActions attemptLogin(String email, String password) throws Exception {
        return mvc.perform(post("/api/session")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }

    protected RequestBuilder changeStatus(MockHttpSession session, String task, String status, long version) {
        return put("/api/tasks/{id}/status", ids.get(task)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"%s\", \"version\": %d}".formatted(status, version));
    }
}
