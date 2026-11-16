package pt.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;

import pt.matriz.ApiTestSupport;
import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Ficha PL 04, exercício 5: controlo de acesso às tarefas entre equipas (IDOR / Broken Access
 * Control, SEG02). Cenários CT13 a CT19, CT21, CT31, CT32 e CT34 de docs/test-plan.md.
 * <p>
 * Uma pessoa autenticada muda o identificador no pedido para chegar a uma tarefa de um projeto que
 * não vê. Pela DA17, um recurso que existe mas não é visível dá 403, e a permissão verifica-se antes
 * da validação (o 403 vem antes do 400 e do 409). Cada teste confirma também, na base de dados, que
 * nada mudou.
 * <p>
 * Logística Norte: Rui (gere o Novo armazém), Marta e Pedro. Compras: Tiago (gere Fornecedores),
 * Marta e João. A tarefa "N1" é do Novo armazém e tem a Marta como responsável; "F1" é de
 * Fornecedores e tem o João.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class AccessControlApiTests extends ApiTestSupport {

    private static final String NO_ACCESS = "Não tem acesso a esta página.";
    private static final String FORBIDDEN = "Não tem permissão para esta operação.";

    AccessControlApiTests() {
        super("10.9.0.9");
    }

    @BeforeEach
    void setUp() {
        user("ana", "Ana", "ADMINISTRATOR");
        user("rui", "Rui", "MANAGER");
        user("tiago", "Tiago", "MANAGER");
        user("marta", "Marta", "MEMBER");
        user("pedro", "Pedro", "MEMBER");
        user("joao", "João", "MEMBER");

        team("norte", "Logística Norte", "rui", "marta", "pedro");
        team("compras", "Compras", "tiago", "marta", "joao");

        project("novo", "Novo armazém", "norte", "rui", "ACTIVE");
        project("fornecedores", "Fornecedores", "compras", "tiago", "ACTIVE");

        task("N1", "novo", "TODO", "MEDIUM", null, "marta");
        task("F1", "fornecedores", "TODO", "MEDIUM", null, "joao");
    }

    @Test
    void memberOfAnotherTeamCannotEditTheTask() throws Exception {
        // CT13: o Pedro (Logística Norte) tenta editar uma tarefa de Fornecedores (Compras).
        send(patch("/api/tasks/{id}", ids.get("F1")), "pedro", """
                {"title": "Alterado pelo Pedro", "version": 0}""")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.detail").value(FORBIDDEN))
                // Nenhum campo da tarefa na resposta. "title" e "status" não servem para isto, porque
                // também são campos do Problem Details (RFC 9457).
                .andExpect(jsonPath("$.projectId").doesNotExist())
                .andExpect(jsonPath("$.assignees").doesNotExist());
        assertThat(column("title", "F1")).isEqualTo("F1");
        assertThat(column("version", "F1")).isEqualTo("0");
    }

    @Test
    void memberOfAnotherTeamCannotChangeTheStatus() throws Exception {
        // CT14: o João (Compras) tenta concluir uma tarefa do Novo armazém (Logística Norte).
        send(put("/api/tasks/{id}/status", ids.get("N1")), "joao", """
                {"status": "DONE", "version": 0}""")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(column("status", "N1")).isEqualTo("TODO");
    }

    @Test
    void managerOfAnotherTeamCannotDeleteTheTask() throws Exception {
        // CT15: o Tiago gere Fornecedores, mas não o Novo armazém.
        send(delete("/api/tasks/{id}", ids.get("N1")), "tiago", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(column("deleted_at", "N1")).isNull();
    }

    @Test
    void managerCannotReadOrCreateTasksInAProjectOfAnotherTeam() throws Exception {
        // CT16: o Rui troca o identificador do projeto pelo de Fornecedores.
        int tasksBefore = countRows("tasks");
        mvc.perform(get("/api/projects/{id}/tasks", ids.get("fornecedores")).session(login("rui")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(NO_ACCESS))
                .andExpect(jsonPath("$.items").doesNotExist());
        send(post("/api/projects/{id}/tasks", ids.get("fornecedores")), "rui", """
                {"title": "Tarefa do Rui"}""")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(FORBIDDEN));
        assertThat(countRows("tasks")).isEqualTo(tasksBefore);
    }

    @Test
    void invalidDataDoesNotRevealTheValidationOfAnInvisibleTask() throws Exception {
        // CT17: o 403 vem antes do 400 (DA17); a resposta não traz erros por campo.
        send(patch("/api/tasks/{id}", ids.get("F1")), "pedro", """
                {"title": "", "version": 0}""")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void memberOfAnotherTeamCannotReadOrWriteComments() throws Exception {
        // CT18: o João não comenta nem lê os comentários de uma tarefa do Novo armazém.
        mvc.perform(get("/api/tasks/{id}/comments", ids.get("N1")).session(login("joao")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(NO_ACCESS))
                .andExpect(jsonPath("$.items").doesNotExist());
        send(post("/api/tasks/{id}/comments", ids.get("N1")), "joao", """
                {"content": "Comentário do João"}""")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(countRows("task_comments")).isZero();
    }

    @Test
    void managerOfAnotherTeamCannotAssignPeople() throws Exception {
        // CT19: 403 e não 409 INVALID_ASSIGNEE (o João não pertence à Logística Norte), pela ordem de DA17.
        send(put("/api/tasks/{id}/assignees/{userId}", ids.get("N1"), ids.get("joao")), "tiago", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM task_assignees WHERE task_id = ? AND removed_at IS NULL",
                Integer.class, ids.get("N1"))).isEqualTo(1);
    }

    @Test
    void memberOfAnotherTeamCannotFindOutWhoIsAssigned() throws Exception {
        // Regressão de F1 de quality-security-review.md: o Pedro (Logística Norte) recebe a mesma
        // resposta, 403, quer a pessoa seja responsável pela tarefa de Fornecedores (João) quer não
        // (Marta). Antes, a Marta dava 404 "Esta pessoa não é responsável pela tarefa.".
        send(delete("/api/tasks/{id}/assignees/{userId}", ids.get("F1"), ids.get("joao")), "pedro", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        send(delete("/api/tasks/{id}/assignees/{userId}", ids.get("F1"), ids.get("marta")), "pedro", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM task_assignees WHERE task_id = ? AND user_id = ? AND removed_at IS NULL",
                Integer.class, ids.get("F1"), ids.get("joao"))).isEqualTo(1);
    }

    @Test
    void requestWithoutSessionIsRefused() throws Exception {
        // CT21: sem sessão, nenhuma operação sobre tarefas responde com dados.
        mvc.perform(get("/api/tasks/{id}", ids.get("N1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.projectId").doesNotExist())
                .andExpect(jsonPath("$.assignees").doesNotExist());
        mvc.perform(get("/api/projects/{id}/tasks", ids.get("novo")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void unknownFieldsWhenCreatingHaveNoEffect() throws Exception {
        // CT31 (mass assignment): os campos que não pertencem a CreateTaskRequest não mudam o projeto,
        // o estado nem os responsáveis da tarefa criada.
        send(post("/api/projects/{id}/tasks", ids.get("novo")), "rui", """
                {"title": "Campos a mais", "projectId": %d, "status": "DONE", "version": 7,
                 "assignees": [{"id": %d}], "deletedAt": "2026-11-01T10:00:00Z"}"""
                .formatted(ids.get("fornecedores"), ids.get("pedro")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(ids.get("novo")))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.assignees", empty()));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM tasks
                WHERE title = 'Campos a mais' AND project_id = ? AND status = 'TODO' AND deleted_at IS NULL""",
                Integer.class, ids.get("novo"))).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks WHERE project_id = ?", Integer.class,
                ids.get("fornecedores"))).isEqualTo(1);
    }

    @Test
    void changeWithoutCsrfTokenIsRefused() throws Exception {
        // CT32: com sessão válida mas sem o token CSRF (DA04), o pedido é recusado e nada é gravado.
        int tasksBefore = countRows("tasks");
        mvc.perform(post("/api/projects/{id}/tasks", ids.get("novo")).session(login("rui"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Sem token\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(countRows("tasks")).isEqualTo(tasksBefore);
    }

    // ───── Pedidos e consultas ─────

    /** Pedido com sessão da pessoa {@code who} e token CSRF; {@code body} pode ser nulo. */
    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String who, String body) throws Exception {
        request.session(login(who)).with(csrf());
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(request);
    }

    private String column(String column, String task) {
        return jdbc.queryForObject("SELECT " + column + "::text FROM tasks WHERE id = ?", String.class, ids.get(task));
    }

    private int countRows(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
