package pt.matriz.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
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
 * Incremento I7 (resto do MVP): contas pela API real. Cenários de US02, US14 e US18, com as
 * consequências noutros módulos (RN16, RN17, RN22, RN23) e o último Administrador (RN18).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class AccountApiTests extends ApiTestSupport {

    private static final String TEMPORARY = "temporaria-123";

    AccountApiTests() {
        super("10.9.0.5");
    }

    @BeforeEach
    void setUp() {
        user("ana", "Ana", "ADMINISTRATOR");
        user("rui", "Rui", "MANAGER");
        user("sofia", "Sofia", "MANAGER");
        user("tiago", "Tiago", "MANAGER");
        user("marta", "Marta", "MEMBER");
        user("pedro", "Pedro", "MEMBER");
        user("carlos", "Carlos", "OBSERVER");

        team("norte", "Logística Norte", "rui", "sofia", "marta", "pedro");
        team("compras", "Compras", "tiago", "marta");

        project("novo", "Novo armazém", "norte", "rui", "ACTIVE");
        project("sul", "Armazém Sul", "norte", "rui", "ARCHIVED");
        project("frota", "Frota", "norte", "sofia", "ACTIVE");
        project("fornecedores", "Fornecedores", "compras", "tiago", "ACTIVE");

        task("P1", "novo", "TODO", "MEDIUM", null, "pedro");
        task("P2", "novo", "IN_PROGRESS", "MEDIUM", null, "pedro", "marta");
        task("D1", "novo", "DONE", "LOW", null, "pedro");
        task("D2", "novo", "DONE", "LOW", null, "pedro");
        task("D3", "novo", "DONE", "LOW", null, "pedro");
        task("D4", "novo", "DONE", "LOW", null, "pedro");
        task("S1", "sul", "TODO", "MEDIUM", null, "pedro");
        task("M1", "novo", "TODO", "HIGH", null, "marta");
        task("M3", "novo", "IN_PROGRESS", "LOW", null, "marta");
        task("F1", "fornecedores", "TODO", "MEDIUM", null, "marta");
    }

    // ───── US02 Criar e editar conta ─────

    @Test
    void administratorCreatesAnAccountWithATemporaryPassword() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(createUser(ana, "Inês Costa", "Ines@Empresa.example", "MEMBER", "a".repeat(12)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ines@empresa.example"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        mvc.perform(get("/api/users").session(ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name").value(hasItem("Inês Costa")));

        MockHttpSession ines = (MockHttpSession) attemptLogin("ines@empresa.example", "a".repeat(12))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/session").session(ines))
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    void invalidAccountDataIsRefused() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(createUser(ana, "Inês Costa", "ines@empresa.example", "MEMBER", "a".repeat(11)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A palavra-passe tem de ter entre 12 e 128 caracteres."));
        mvc.perform(createUser(ana, "n".repeat(101), "ines@empresa.example", "MEMBER", TEMPORARY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        mvc.perform(createUser(ana, "   ", "ines@empresa.example", "MEMBER", TEMPORARY))
                .andExpect(status().isBadRequest());
        mvc.perform(createUser(ana, "Inês Costa", "", "MEMBER", TEMPORARY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        mvc.perform(createUser(ana, "Inês Costa", "ines.empresa.example", "MEMBER", TEMPORARY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O email não é válido."));
        mvc.perform(post("/api/users").session(ana).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Inês Costa", "email": "ines@empresa.example", "temporaryPassword": "%s"}"""
                                .formatted(TEMPORARY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("role"));
        assertThat(userCount()).isEqualTo(7);

        mvc.perform(createUser(ana, "n".repeat(100), "ines@empresa.example", "MEMBER", TEMPORARY))
                .andExpect(status().isCreated());
    }

    @Test
    void emailIsUniqueIgnoringCase() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(createUser(ana, "Outra Marta", "MARTA@empresa.example", "MEMBER", TEMPORARY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.detail").value("Já existe uma conta com este email"));
        mvc.perform(updateUser(ana, "marta", """
                        {"email": "pedro@empresa.example"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void editedEmailIsUsedToLogIn() throws Exception {
        mvc.perform(updateUser(login("ana"), "marta", """
                        {"email": "Marta.Silva@empresa.example", "name": "Marta Silva"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("marta.silva@empresa.example"))
                .andExpect(jsonPath("$.name").value("Marta Silva"));
        attemptLogin("marta.silva@empresa.example", PASSWORD).andExpect(status().isOk());
        attemptLogin("marta@empresa.example", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyTheAdministratorManagesAccounts() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(get("/api/users").session(rui))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Não tem acesso a esta página."));
        mvc.perform(get("/api/users").session(login("carlos"))).andExpect(status().isForbidden());
        mvc.perform(createUser(rui, "Inês Costa", "ines@empresa.example", "MEMBER", TEMPORARY))
                .andExpect(status().isForbidden());
        mvc.perform(updateUser(rui, "marta", """
                        {"role": "MANAGER"}"""))
                .andExpect(status().isForbidden());
        mvc.perform(changeAccountStatus(rui, "pedro", "DEACTIVATED")).andExpect(status().isForbidden());
        mvc.perform(resetPassword(rui, "pedro", TEMPORARY)).andExpect(status().isForbidden());
    }

    @Test
    void listFiltersByStatusAndRole() throws Exception {
        jdbc.update("UPDATE users SET status = 'DEACTIVATED' WHERE id = ?", ids.get("pedro"));
        mvc.perform(get("/api/users").param("status", "ACTIVE").param("role", "MANAGER").param("role", "MEMBER")
                        .session(login("ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].name").value(contains("Marta", "Rui", "Sofia", "Tiago")));
    }

    @Test
    void editRequestsAreValidated() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(updateUser(ana, "marta", """
                        {"status": "DEACTIVATED"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
        mvc.perform(updateUser(ana, "marta", "{}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/users/{id}", 999_999).session(ana).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void resettingAccessUnblocksTheEmailAndRequiresANewPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            attemptLogin("pedro@empresa.example", "palavra-errada-123").andExpect(status().isUnauthorized());
        }
        attemptLogin("pedro@empresa.example", PASSWORD).andExpect(status().isTooManyRequests());

        mvc.perform(resetPassword(login("ana"), "pedro", TEMPORARY)).andExpect(status().isNoContent());

        attemptLogin("pedro@empresa.example", PASSWORD).andExpect(status().isUnauthorized());
        MockHttpSession pedro = (MockHttpSession) attemptLogin("pedro@empresa.example", TEMPORARY)
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/session").session(pedro)).andExpect(jsonPath("$.mustChangePassword").value(true));
        mvc.perform(resetPassword(login("ana"), "pedro", "curta")).andExpect(status().isBadRequest());
    }

    // ───── US14 Desativar e reativar conta ─────

    @Test
    void deactivatingRemovesTheOpenTasksAndKeepsTheCompletedOnes() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(changeAccountStatus(ana, "pedro", "DEACTIVATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));

        attemptLogin("pedro@empresa.example", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"));
        mvc.perform(get("/api/tasks/{id}", ids.get("P1")).session(ana))
                .andExpect(jsonPath("$.assignees").value(empty()));
        mvc.perform(get("/api/tasks/{id}", ids.get("P2")).session(ana))
                .andExpect(jsonPath("$.assignees[*].name").value(contains("Marta")));
        assertThat(activeAssignments("pedro", "DONE")).isEqualTo(4);
        // Exceção de RN09: a tarefa do projeto arquivado também fica sem o Pedro (E22).
        mvc.perform(get("/api/tasks/{id}", ids.get("S1")).session(ana))
                .andExpect(jsonPath("$.assignees").value(empty()));
    }

    @Test
    void deactivatingEndsTheOpenSession() throws Exception {
        MockHttpSession pedro = login("pedro");
        mvc.perform(changeAccountStatus(login("ana"), "pedro", "DEACTIVATED")).andExpect(status().isOk());
        mvc.perform(get("/api/projects").session(pedro)).andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatingTheManagerLeavesTheProjectsWithoutManager() throws Exception {
        mvc.perform(changeAccountStatus(login("ana"), "rui", "DEACTIVATED")).andExpect(status().isOk());
        assertThat(managerOf("novo")).isNull();
        assertThat(managerOf("sul")).isNull();
        assertThat(managerOf("frota")).isEqualTo(ids.get("sofia"));
    }

    @Test
    void reactivatedAccountKeepsRoleAndTeamsButNotTheTasks() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(changeAccountStatus(ana, "pedro", "DEACTIVATED")).andExpect(status().isOk());
        mvc.perform(changeAccountStatus(ana, "pedro", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        login("pedro");
        mvc.perform(get("/api/teams/{id}/members", ids.get("norte")).session(ana))
                .andExpect(jsonPath("$.items[*].name").value(hasItem("Pedro")));
        mvc.perform(get("/api/tasks/{id}", ids.get("P1")).session(ana))
                .andExpect(jsonPath("$.assignees").value(empty()));
    }

    @Test
    void theLastActiveAdministratorStays() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(changeAccountStatus(ana, "ana", "DEACTIVATED"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ADMINISTRATOR"))
                .andExpect(jsonPath("$.detail").value("Tem de existir pelo menos um administrador ativo"));
        mvc.perform(updateUser(ana, "ana", """
                        {"role": "MANAGER"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ADMINISTRATOR"));

        user("bia", "Bia", "ADMINISTRATOR");
        mvc.perform(changeAccountStatus(ana, "ana", "DEACTIVATED")).andExpect(status().isOk());
    }

    // ───── US18 Mudar papel de uma conta ─────

    @Test
    void managerWhoBecomesMemberLeavesTheProjectsWithoutManager() throws Exception {
        mvc.perform(updateUser(login("ana"), "rui", """
                        {"role": "MEMBER"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));
        assertThat(managerOf("novo")).isNull();
        assertThat(managerOf("sul")).isNull();
        assertThat(managerOf("frota")).isEqualTo(ids.get("sofia"));
        // Como Membro, continua na equipa.
        assertThat(activeTeams("rui")).isEqualTo(1);
    }

    @Test
    void memberWhoBecomesObserverLeavesTheTeamsAndTheOpenTasks() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(updateUser(ana, "marta", """
                        {"role": "OBSERVER"}"""))
                .andExpect(status().isOk());
        assertThat(activeTeams("marta")).isZero();
        assertThat(activeAssignments("marta", "TODO")).isZero();
        assertThat(activeAssignments("marta", "IN_PROGRESS")).isZero();
        mvc.perform(get("/api/tasks/{id}", ids.get("P2")).session(ana))
                .andExpect(jsonPath("$.assignees[*].name").value(contains("Pedro")));
    }

    @Test
    void roleChangeTakesEffectInTheNextRequest() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(updateUser(login("ana"), "rui", """
                        {"role": "MEMBER"}"""))
                .andExpect(status().isOk());
        mvc.perform(post("/api/projects/{id}/tasks", ids.get("novo")).session(rui).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Nova\"}"))
                .andExpect(status().isForbidden());
    }

    // ───── Apoio ─────

    private int userCount() {
        return jdbc.queryForObject("SELECT count(*) FROM users", Integer.class);
    }

    private Long managerOf(String project) {
        return jdbc.queryForObject("SELECT manager_id FROM projects WHERE id = ?", Long.class, ids.get(project));
    }

    private int activeTeams(String person) {
        return jdbc.queryForObject("SELECT count(*) FROM team_members WHERE user_id = ? AND removed_at IS NULL",
                Integer.class, ids.get(person));
    }

    private int activeAssignments(String person, String taskStatus) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM task_assignees a JOIN tasks t ON t.id = a.task_id
                WHERE a.user_id = ? AND a.removed_at IS NULL AND t.status = ?
                """, Integer.class, ids.get(person), taskStatus);
    }

    private static RequestBuilder createUser(MockHttpSession session, String name, String email, String role,
            String password) {
        return post("/api/users").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "email": "%s", "role": "%s", "temporaryPassword": "%s"}"""
                        .formatted(name, email, role, password));
    }

    private RequestBuilder updateUser(MockHttpSession session, String person, String body) {
        return patch("/api/users/{id}", ids.get(person)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private RequestBuilder changeAccountStatus(MockHttpSession session, String person, String status) {
        return put("/api/users/{id}/status", ids.get(person)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"%s\"}".formatted(status));
    }

    private RequestBuilder resetPassword(MockHttpSession session, String person, String password) {
        return put("/api/users/{id}/password", ids.get(person)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"temporaryPassword\": \"%s\"}".formatted(password));
    }
}
