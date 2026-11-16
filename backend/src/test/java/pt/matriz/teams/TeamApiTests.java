package pt.matriz.teams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.RequestBuilder;

import pt.matriz.ApiTestSupport;
import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Incremento I7 (resto do MVP): equipas pela API real. Cenários de US03, com as consequências de
 * remover uma pessoa (RN17, RN22) e a regra de quem pode pertencer a equipas (RN26).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class TeamApiTests extends ApiTestSupport {

    TeamApiTests() {
        super("10.9.0.6");
    }

    @BeforeEach
    void setUp() {
        user("ana", "Ana", "ADMINISTRATOR");
        user("rui", "Rui", "MANAGER");
        user("sofia", "Sofia", "MANAGER");
        user("tiago", "Tiago", "MANAGER");
        user("marta", "Marta", "MEMBER");
        user("pedro", "Pedro", "MEMBER");
        user("joao", "João", "MEMBER");
        user("carlos", "Carlos", "OBSERVER");

        team("norte", "Logística Norte", "rui", "sofia", "marta", "pedro");
        team("compras", "Compras", "tiago", "marta", "joao");

        project("novo", "Novo armazém", "norte", "rui", "ACTIVE");
        project("sul", "Armazém Sul", "norte", "rui", "ARCHIVED");
        project("fornecedores", "Fornecedores", "compras", "tiago", "ACTIVE");

        task("F1", "fornecedores", "TODO", "MEDIUM", null, "marta");
        task("F2", "fornecedores", "IN_PROGRESS", "HIGH", null, "marta");
        task("F3", "fornecedores", "DONE", "LOW", null, "marta");
        task("N1", "novo", "TODO", "MEDIUM", null, "marta");
    }

    @Test
    void administratorCreatesATeam() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(createTeam(ana, "  Armazém Central  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Armazém Central"));
        mvc.perform(get("/api/teams").session(ana))
                .andExpect(jsonPath("$.items[*].name").value(contains("Armazém Central", "Compras", "Logística Norte")));
    }

    @Test
    void invalidOrRepeatedNamesAreRefused() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(createTeam(ana, "")).andExpect(status().isBadRequest());
        mvc.perform(createTeam(ana, "Compras"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TEAM_NAME"))
                .andExpect(jsonPath("$.detail").value("Já existe uma equipa com este nome."));
        mvc.perform(createTeam(ana, "compras")).andExpect(status().isConflict());
        mvc.perform(createTeam(ana, "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        mvc.perform(renameTeam(ana, "norte", "Compras")).andExpect(status().isConflict());
        mvc.perform(renameTeam(ana, "norte", "   ")).andExpect(status().isBadRequest());
        // A própria equipa pode mudar só as maiúsculas.
        mvc.perform(renameTeam(ana, "compras", "COMPRAS")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM teams", Integer.class)).isEqualTo(2);
    }

    @Test
    void renamedTeamKeepsItsProjects() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(renameTeam(ana, "compras", "Compras e Aprovisionamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Compras e Aprovisionamento"));
        mvc.perform(get("/api/projects/{id}", ids.get("fornecedores")).session(ana))
                .andExpect(jsonPath("$.team.name").value("Compras e Aprovisionamento"));
    }

    @Test
    void aPersonCanBelongToTwoTeams() throws Exception {
        mvc.perform(get("/api/teams").session(login("marta")))
                .andExpect(jsonPath("$.items[*].name").value(contains("Compras", "Logística Norte")));
    }

    @Test
    void onlyActiveManagersAndMembersCanBeAdded() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(addMember(ana, "compras", "carlos"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TEAM_MEMBER"));
        mvc.perform(addMember(ana, "norte", "marta"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TEAM_MEMBER"));

        jdbc.update("UPDATE users SET status = 'DEACTIVATED' WHERE id = ?", ids.get("pedro"));
        mvc.perform(addMember(ana, "compras", "pedro")).andExpect(status().isConflict());
        mvc.perform(get("/api/users").param("status", "ACTIVE").param("role", "MANAGER").param("role", "MEMBER")
                        .session(ana))
                .andExpect(jsonPath("$.items[*].name").value(not(hasItem("Pedro"))));

        mvc.perform(addMember(ana, "compras", "rui")).andExpect(status().isNoContent());
        mvc.perform(get("/api/teams/{id}/members", ids.get("compras")).session(ana))
                .andExpect(jsonPath("$.items[*].name").value(contains("João", "Marta", "Rui", "Tiago")));
    }

    @Test
    void removingAPersonRemovesItFromTheOpenTasksOfThatTeam() throws Exception {
        MockHttpSession ana = login("ana");
        mvc.perform(removeMember(ana, "compras", "marta")).andExpect(status().isNoContent());

        mvc.perform(get("/api/tasks/{id}", ids.get("F1")).session(ana)).andExpect(jsonPath("$.assignees").value(empty()));
        mvc.perform(get("/api/tasks/{id}", ids.get("F2")).session(ana)).andExpect(jsonPath("$.assignees").value(empty()));
        mvc.perform(get("/api/tasks/{id}", ids.get("F3")).session(ana))
                .andExpect(jsonPath("$.assignees[*].name").value(contains("Marta")));
        mvc.perform(get("/api/tasks/{id}", ids.get("N1")).session(ana))
                .andExpect(jsonPath("$.assignees[*].name").value(contains("Marta")));
        mvc.perform(get("/api/teams/{id}/members", ids.get("compras")).session(ana))
                .andExpect(jsonPath("$.items[*].name").value(contains("João", "Tiago")));
        // A pertença fica marcada como removida (RN32).
        assertThat(jdbc.queryForObject("SELECT count(*) FROM team_members WHERE user_id = ? AND removed_at IS NOT NULL",
                Integer.class, ids.get("marta"))).isEqualTo(1);
    }

    @Test
    void removingTheManagerLeavesTheProjectsWithoutManager() throws Exception {
        mvc.perform(removeMember(login("ana"), "norte", "rui")).andExpect(status().isNoContent());
        assertThat(managerOf("novo")).isNull();
        assertThat(managerOf("sul")).isNull();
        assertThat(managerOf("fornecedores")).isEqualTo(ids.get("tiago"));
    }

    @Test
    void membersAreVisibleOnlyInTheTeamsOfThePerson() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(get("/api/teams/{id}/members", ids.get("norte")).session(rui))
                .andExpect(jsonPath("$.items[*].name").value(contains("Marta", "Pedro", "Rui", "Sofia")));
        mvc.perform(get("/api/teams/{id}/members", ids.get("compras")).session(rui))
                .andExpect(status().isForbidden());
    }

    @Test
    void removalTakesEffectInTheNextRequest() throws Exception {
        MockHttpSession pedro = login("pedro");
        mvc.perform(removeMember(login("ana"), "norte", "pedro")).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/{id}", ids.get("novo")).session(pedro)).andExpect(status().isForbidden());
    }

    @Test
    void onlyTheAdministratorManagesTeams() throws Exception {
        MockHttpSession rui = login("rui");
        mvc.perform(createTeam(rui, "Armazém Central")).andExpect(status().isForbidden());
        mvc.perform(renameTeam(rui, "norte", "Norte")).andExpect(status().isForbidden());
        mvc.perform(addMember(rui, "norte", "joao")).andExpect(status().isForbidden());
        mvc.perform(removeMember(rui, "norte", "pedro")).andExpect(status().isForbidden());

        MockHttpSession ana = login("ana");
        mvc.perform(patch("/api/teams/{id}", 999_999).session(ana).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"X\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(removeMember(ana, "norte", "joao"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("A pessoa não pertence à equipa."));
    }

    private Long managerOf(String project) {
        return jdbc.queryForObject("SELECT manager_id FROM projects WHERE id = ?", Long.class, ids.get(project));
    }

    private static RequestBuilder createTeam(MockHttpSession session, String name) {
        return post("/api/teams").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"%s\"}".formatted(name));
    }

    private RequestBuilder renameTeam(MockHttpSession session, String team, String name) {
        return patch("/api/teams/{id}", ids.get(team)).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"%s\"}".formatted(name));
    }

    private RequestBuilder addMember(MockHttpSession session, String team, String person) {
        return put("/api/teams/{id}/members/{userId}", ids.get(team), ids.get(person)).session(session).with(csrf());
    }

    private RequestBuilder removeMember(MockHttpSession session, String team, String person) {
        return delete("/api/teams/{id}/members/{userId}", ids.get(team), ids.get(person)).session(session).with(csrf());
    }
}
