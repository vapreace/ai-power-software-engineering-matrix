package pt.matriz.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Regressão do bug B3 (docs/verification.md): no {@code .jar} de entrega, abrir ou recarregar um
 * endereço da aplicação, como {@code /projetos/1}, dava 404. O teste usa um {@code index.html}
 * de teste em {@code src/test/resources/static}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class SpaFallbackTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void rootUsesTheWelcomePage() throws Exception {
        // A raiz é tratada pela página inicial do Spring Boot, que reencaminha para o index.html.
        // O MockMvc não executa o reencaminhamento: confirma-se só o destino.
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void applicationAddressesReturnTheFrontend() throws Exception {
        for (String address : new String[] {"/projetos", "/projetos/1", "/tarefas/9/editar", "/entrar"}) {
            mvc.perform(get(address))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("frontend-de-teste")));
        }
    }

    @Test
    void apiRequestsNeverReturnTheFrontend() throws Exception {
        mvc.perform(get("/api/inexistente"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("frontend-de-teste"))));
    }

    @Test
    void missingFilesWithAnExtensionAreNotReplaced() throws Exception {
        mvc.perform(get("/assets/nao-existe.js"))
                .andExpect(status().isNotFound());
    }
}
