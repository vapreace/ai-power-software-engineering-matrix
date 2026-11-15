package pt.matriz.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Incremento I2 (Ficha PL 03, exercício 4): gestão de projetos pela API real, com PostgreSQL
 * e a data atual fixa em 2026-11-10 (DA31). Inclui os três testes pedidos pela ficha:
 * validação, autorização e criação com consulta posterior.
 * <p>
 * Situação de partida de user-stories.md. "Novo armazém" tem 3 tarefas: uma concluída,
 * uma atrasada (prazo 2026-11-09) e uma com prazo igual à data atual, que não está atrasada (E2).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class ProjectApiTests {

    private static final String PASSWORD = "palavra-passe-de-teste";
    private static final String ADDRESS = "10.9.0.1";

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
        project("fornecedores", "Fornecedores", "compras", "tiago", "ACTIVE");

        task("novo", "Escolher localização", "2026-10-20", "DONE");
        task("novo", "Contratar transporte", "2026-11-09", "TODO");
        task("novo", "Pedir orçamento de estantes", "2026-11-10", "IN_PROGRESS");
    }

    // ───── Os três testes da ficha (DA31) ─────

    @Test
    void validationRefusesAProjectNameWithOnlySpaces() throws Exception {
        mvc.perform(createProject(login("rui"), """
                        {"teamId": %d, "name": "   ", "startDate": "2026-11-12"}""".formatted(ids.get("norte"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.detail").value("O nome é obrigatório e tem no máximo 100 caracteres."));
        assertThat(projectCount()).isEqualTo(4);
    }

    @Test
    void authorizationRefusesMembersAndProjectsOfOtherTeams() throws Exception {
        mvc.perform(createProject(login("marta"), validProject("norte", "Inventário 2026")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.detail").value("Não tem permissão para esta operação."));

        mvc.perform(get("/api/projects/{id}", ids.get("fornecedores")).session(login("pedro")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Não tem acesso a esta página."));
        assertThat(projectCount()).isEqualTo(4);
    }

    @Test
    void createdProjectCanBeReadInALaterRequest() throws Exception {
        MvcResult created = mvc.perform(createProject(login("rui"), """
                        {"teamId": %d, "name": "  Inventário 2026  ", "startDate": "2026-11-12",
                         "expectedEndDate": "2026-12-18", "managerId": %d}"""
                        .formatted(ids.get("norte"), ids.get("sofia"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/projects/\\d+")))
                .andReturn();
        Number id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // Um pedido novo, com outra sessão: o projeto ficou guardado (US04).
        mvc.perform(get("/api/projects/{id}", id).session(login("pedro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Inventário 2026"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.manager.name").value("Rui"))
                .andExpect(jsonPath("$.team.name").value("Logística Norte"))
                .andExpect(jsonPath("$.startDate").value("2026-11-12"))
                .andExpect(jsonPath("$.expectedEndDate").value("2026-12-18"))
                .andExpect(jsonPath("$.progress.percentage").isEmpty())
                .andExpect(jsonPath("$.version").value(0));
        mvc.perform(get("/api/projects").session(login("pedro")))
                .andExpect(jsonPath("$.items[*].name").value(contains("Frota", "Inventário 2026", "Novo armazém")));
    }

    // ───── US04 Criar projeto ─────

    @Test
    void administratorMustIndicateAValidManager() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(createProject(ana, validProject("compras", "Auditoria")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("managerId"));

        mvc.perform(createProject(ana, """
                        {"teamId": %d, "name": "Auditoria", "startDate": "2026-11-12", "managerId": %d}"""
                        .formatted(ids.get("norte"), ids.get("pedro"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_MANAGER"));

        mvc.perform(createProject(ana, """
                        {"teamId": %d, "name": "Auditoria", "startDate": "2026-11-12", "managerId": %d}"""
                        .formatted(ids.get("compras"), ids.get("tiago"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.manager.name").value("Tiago"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void teamWithoutAnActiveManagerHasNoNewProjects() throws Exception {
        team("central", "Armazém Central", "pedro");
        mvc.perform(createProject(login("ana"), """
                        {"teamId": %d, "name": "Arranque", "startDate": "2026-11-12", "managerId": %d}"""
                        .formatted(ids.get("central"), ids.get("rui"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_WITHOUT_MANAGER"))
                .andExpect(jsonPath("$.detail").value("A equipa não tem nenhum Gestor"));

        jdbc.update("UPDATE users SET status = 'DEACTIVATED' WHERE id = ?", ids.get("tiago"));
        mvc.perform(createProject(login("ana"), """
                        {"teamId": %d, "name": "Arranque", "startDate": "2026-11-12", "managerId": %d}"""
                        .formatted(ids.get("compras"), ids.get("tiago"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEAM_WITHOUT_MANAGER"));
    }

    @Test
    void managerOnlyCreatesInTheirTeamsAndDataMustBeCoherent() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(createProject(rui, validProject("compras", "Inventário 2026")))
                .andExpect(status().isForbidden());
        mvc.perform(createProject(rui, """
                        {"teamId": %d, "name": "Inventário 2026", "startDate": "2026-12-18", "expectedEndDate": "2026-11-12"}"""
                        .formatted(ids.get("norte"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("expectedEndDate"));
        mvc.perform(createProject(rui, """
                        {"teamId": %d, "name": "Inventário 2026"}""".formatted(ids.get("norte"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startDate"));
        mvc.perform(createProject(rui, validProject("norte", "é".repeat(100))))
                .andExpect(status().isCreated());
        mvc.perform(createProject(rui, validProject("norte", "é".repeat(101))))
                .andExpect(status().isBadRequest());
        mvc.perform(createProject(rui, """
                        {"teamId": %d, "name": "Com descrição", "startDate": "2026-11-12", "description": "%s"}"""
                        .formatted(ids.get("norte"), "d".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("description"));
    }

    // ───── US17 Editar projeto ─────

    @Test
    void managerEditsTheirProjectAndVersionIncreases() throws Exception {
        mvc.perform(updateProject(login("rui"), "novo", """
                        {"name": "Armazém Norte", "expectedEndDate": "2027-01-15", "version": 0}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Armazém Norte"))
                .andExpect(jsonPath("$.expectedEndDate").value("2027-01-15"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.progress.completedTasks").value(1));

        mvc.perform(updateProject(login("rui"), "frota", """
                        {"name": "Frota nova", "version": 0}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void closedProjectCannotBeEdited() throws Exception {
        mvc.perform(updateProject(login("rui"), "sul", """
                        {"name": "Armazém Sul 2", "version": 0}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_CLOSED"));
    }

    @Test
    void savingOverAnOldVersionIsRefused() throws Exception {
        mvc.perform(updateProject(login("ana"), "novo", """
                        {"expectedEndDate": "2027-02-01", "version": 0}"""))
                .andExpect(status().isOk());

        mvc.perform(updateProject(login("rui"), "novo", """
                        {"name": "Armazém Norte", "version": 0}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"))
                .andExpect(jsonPath("$.detail")
                        .value("Este projeto foi alterado por outra pessoa. Abra-o de novo e repita a alteração."));
        assertThat(jdbc.queryForObject("SELECT name FROM projects WHERE id = ?", String.class, ids.get("novo")))
                .isEqualTo("Novo armazém");
    }

    @Test
    void teamCannotBeChangedAndManagerlessProjectIsOnlyEditedByTheAdministrator() throws Exception {
        mvc.perform(updateProject(login("ana"), "novo", """
                        {"teamId": %d, "version": 0}""".formatted(ids.get("compras"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("teamId"));

        jdbc.update("UPDATE projects SET manager_id = NULL WHERE id = ?", ids.get("novo"));
        mvc.perform(updateProject(login("rui"), "novo", """
                        {"name": "Armazém Norte", "version": 0}"""))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/{id}", ids.get("novo")).session(login("rui")))
                .andExpect(jsonPath("$.manager").isEmpty());
    }

    // ───── US20 Eliminar projeto ─────

    @Test
    void deletingKeepsTheDataButHidesTheProjectAndItsTasks() throws Exception {
        mvc.perform(delete("/api/projects/{id}", ids.get("novo")).session(login("marta")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/projects/{id}", ids.get("novo")).session(login("sofia")).with(csrf()))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/projects/{id}", ids.get("novo")).session(login("rui")).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/projects/{id}", ids.get("novo")).session(login("marta")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("O projeto não existe."));
        mvc.perform(get("/api/projects").session(login("rui")))
                .andExpect(jsonPath("$.items[*].name").value(contains("Frota")));

        // Nada foi apagado (RN32): o projeto e as tarefas estão marcados com a mesma data e hora.
        assertThat(projectCount()).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM tasks t JOIN projects p ON p.id = t.project_id "
                        + "WHERE p.id = ? AND t.deleted_at = p.deleted_at", Integer.class, ids.get("novo")))
                .isEqualTo(3);
    }

    @Test
    void archivedProjectCanBeDeleted() throws Exception {
        mvc.perform(delete("/api/projects/{id}", ids.get("sul")).session(login("rui")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/projects").param("status", "ARCHIVED").session(login("rui")))
                .andExpect(jsonPath("$.items", empty()));
    }

    // ───── US12, US15, US22 Lista, visibilidade e pesquisa ─────

    @Test
    void listShowsVisibleProjectsWithProgressAndOverdueTasks() throws Exception {
        mvc.perform(get("/api/projects").session(login("pedro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name").value(contains("Frota", "Novo armazém")))
                .andExpect(jsonPath("$.items[0].progress.percentage").isEmpty())
                .andExpect(jsonPath("$.items[0].overdueTaskCount").value(0))
                .andExpect(jsonPath("$.items[1].manager.name").value("Rui"))
                .andExpect(jsonPath("$.items[1].progress.completedTasks").value(1))
                .andExpect(jsonPath("$.items[1].progress.totalTasks").value(3))
                .andExpect(jsonPath("$.items[1].progress.percentage").value(33))
                .andExpect(jsonPath("$.items[1].overdueTaskCount").value(1));

        mvc.perform(get("/api/projects").session(login("carlos")))
                .andExpect(jsonPath("$.items[*].name").value(contains("Fornecedores", "Frota", "Novo armazém")));
        mvc.perform(get("/api/projects").param("status", "ARCHIVED").session(login("pedro")))
                .andExpect(jsonPath("$.items[*].name").value(contains("Armazém Sul")));
    }

    @Test
    void searchIgnoresCaseAccentsAndSpacesAndOnlyReturnsVisibleProjects() throws Exception {
        MockHttpSession marta = login("marta");
        mvc.perform(get("/api/projects").param("q", "ARMAZEM").session(marta))
                .andExpect(jsonPath("$.items[*].name").value(contains("Novo armazém")));
        mvc.perform(get("/api/projects").param("q", "  frota  ").session(marta))
                .andExpect(jsonPath("$.items[*].name").value(contains("Frota")));
        mvc.perform(get("/api/projects").param("q", "armazém").param("status", "ACTIVE", "ARCHIVED").session(marta))
                .andExpect(jsonPath("$.items[*].name").value(contains("Armazém Sul", "Novo armazém")));
        mvc.perform(get("/api/projects").param("q", "%").session(marta))
                .andExpect(jsonPath("$.items", empty()));
        mvc.perform(get("/api/projects").param("q", "armazém").session(login("joao")))
                .andExpect(jsonPath("$.items", empty()));
    }

    @Test
    void emptyStatusParameterUsesTheDefaultFilter() throws Exception {
        // Regressão do bug B4 (docs/verification.md): "?status=" dava erro 500.
        mvc.perform(get("/api/projects").param("status", "").session(login("pedro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name").value(contains("Frota", "Novo armazém")));
    }

    // ───── Equipas para o formulário de projeto ─────

    @Test
    void teamsAndMembersRespectVisibility() throws Exception {
        mvc.perform(get("/api/teams").session(login("rui")))
                .andExpect(jsonPath("$.items[*].name").value(contains("Logística Norte")));
        mvc.perform(get("/api/teams").session(login("ana")))
                .andExpect(jsonPath("$.items[*].name").value(contains("Compras", "Logística Norte")));
        mvc.perform(get("/api/teams/{id}/members", ids.get("compras")).session(login("rui")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/teams/{id}/members", ids.get("norte")).session(login("ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name").value(contains("Marta", "Pedro", "Rui", "Sofia")))
                .andExpect(jsonPath("$.items[2].role").value("MANAGER"))
                .andExpect(jsonPath("$.items[2].status").value("ACTIVE"));
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

    private void task(String project, String title, String dueDate, String status) {
        jdbc.update("INSERT INTO tasks (project_id, title, due_date, status) VALUES (?, ?, CAST(? AS DATE), ?)",
                ids.get(project), title, dueDate, status);
    }

    private int projectCount() {
        return jdbc.queryForObject("SELECT count(*) FROM projects", Integer.class);
    }

    private String validProject(String team, String name) {
        return """
                {"teamId": %d, "name": "%s", "startDate": "2026-11-12", "expectedEndDate": "2026-12-18"}"""
                .formatted(ids.get(team), name);
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

    private static RequestBuilder createProject(MockHttpSession session, String body) {
        return post("/api/projects").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private RequestBuilder updateProject(MockHttpSession session, String project, String body) {
        return patch("/api/projects/{id}", ids.get(project)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
