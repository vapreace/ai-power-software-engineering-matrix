package pt.matriz.access;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Cenários de US01 e dos requisitos de segurança da sessão, pela API real
 * (SEG01, SEG04, SEG05, SEG07, SEG08, SEG09, DA25, DA26, DA35).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class SessionApiTests {

    private static final String PASSWORD = "palavra-passe-de-teste";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mvc;

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

        Long team = jdbc.queryForObject("INSERT INTO teams (name) VALUES ('Logística Norte') RETURNING id", Long.class);
        long marta = insertUser("Marta", "marta@empresa.example", "MEMBER", "ACTIVE", false);
        insertUser("Rui", "rui@empresa.example", "MANAGER", "ACTIVE", false);
        insertUser("Pedro", "pedro@empresa.example", "MEMBER", "DEACTIVATED", false);
        insertUser("Inês", "ines@empresa.example", "MEMBER", "ACTIVE", true);
        jdbc.update("INSERT INTO team_members (team_id, user_id) VALUES (?, ?)", team, marta);
    }

    @Test
    void validCredentialsOpenASession() throws Exception {
        mvc.perform(loginRequest("marta@empresa.example", PASSWORD, "10.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Marta"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.teams[0].name").value("Logística Norte"))
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"marta@empresa.example", "pedro@empresa.example", "ninguem@empresa.example"})
    void wrongPasswordGivesTheSameMessage(String email) throws Exception {
        mvc.perform(loginRequest(email, "palavra-passe-errada", "10.0.1.1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Email ou palavra-passe incorretos"));
    }

    @Test
    void deactivatedAccountWithCorrectPasswordIsRefused() throws Exception {
        mvc.perform(loginRequest("pedro@empresa.example", PASSWORD, "10.0.0.1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DEACTIVATED"))
                .andExpect(jsonPath("$.detail").value("Conta desativada. Contacte o administrador."));
    }

    @Test
    void emailIgnoresCase() throws Exception {
        mvc.perform(loginRequest("Marta@Empresa.example", PASSWORD, "10.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("marta@empresa.example"));
    }

    @Test
    void requestWithoutSessionIsRefused() throws Exception {
        mvc.perform(get("/api/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("A sessão terminou. Entre de novo."));
    }

    @Test
    void temporaryPasswordMustBeChangedBeforeAnythingElse() throws Exception {
        MockHttpSession session = login("ines@empresa.example", "10.0.0.1");

        mvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        mvc.perform(get("/api/dashboard").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        mvc.perform(newPasswordRequest(session, "a".repeat(11)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"))
                .andExpect(jsonPath("$.detail").value("A palavra-passe tem de ter entre 12 e 128 caracteres."));
        mvc.perform(newPasswordRequest(session, "a".repeat(12)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void newPasswordIsOnlyAcceptedWhenTheCurrentOneIsTemporary() throws Exception {
        MockHttpSession session = login("marta@empresa.example", "10.0.0.1");

        mvc.perform(newPasswordRequest(session, "outra-palavra-passe"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void tooManyFailedAttemptsBlockOnlyThatEmailAndAddress() throws Exception {
        for (int attempt = 1; attempt <= 5; attempt++) {
            mvc.perform(loginRequest("marta@empresa.example", "palavra-passe-errada", "10.0.2.2"))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(loginRequest("marta@empresa.example", PASSWORD, "10.0.2.2"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"))
                .andExpect(jsonPath("$.detail").value("Demasiadas tentativas. Tente de novo dentro de 15 minutos."));
        mvc.perform(loginRequest("marta@empresa.example", PASSWORD, "10.0.2.3"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MockHttpSession session = login("marta@empresa.example", "10.0.0.1");

        mvc.perform(delete("/api/session").session(session).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/session").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatingTheAccountEndsTheOpenSession() throws Exception {
        MockHttpSession session = login("marta@empresa.example", "10.0.0.1");

        jdbc.update("UPDATE users SET status = 'DEACTIVATED' WHERE email = 'marta@empresa.example'");

        mvc.perform(get("/api/session").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void twoUsersHaveSeparateSessions() throws Exception {
        MockHttpSession marta = login("marta@empresa.example", "10.0.0.1");
        MockHttpSession rui = login("rui@empresa.example", "10.0.0.1");

        mvc.perform(get("/api/session").session(marta)).andExpect(jsonPath("$.name").value("Marta"));
        mvc.perform(get("/api/session").session(rui)).andExpect(jsonPath("$.name").value("Rui"));
    }

    private long insertUser(String name, String email, String role, String status, boolean mustChangePassword) {
        Long id = jdbc.queryForObject("""
                INSERT INTO users (name, email, password_hash, must_change_password, role, status)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, name, email, passwordEncoder.encode(PASSWORD), mustChangePassword, role, status);
        return id;
    }

    private MockHttpSession login(String email, String remoteAddress) throws Exception {
        MvcResult result = mvc.perform(loginRequest(email, PASSWORD, remoteAddress))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private static org.springframework.test.web.servlet.RequestBuilder loginRequest(
            String email, String password, String remoteAddress) {
        return post("/api/session")
                .with(csrf())
                .with(remoteAddress(remoteAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password));
    }

    private static org.springframework.test.web.servlet.RequestBuilder newPasswordRequest(
            MockHttpSession session, String newPassword) {
        return put("/api/session/password")
                .session(session)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\": \"%s\"}".formatted(newPassword));
    }

    private static RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
