package pt.matriz;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

/**
 * Saúde da aplicação para o Docker, a pipeline e o alojamento (DA38, CT37). Os endereços de
 * saúde respondem sem sessão e sem pormenores dos componentes; os outros endpoints do Actuator
 * não estão expostos.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class HealthEndpointTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void livenessAndReadinessAreUpWithoutSessionAndWithoutDetails() throws Exception {
        for (String address : new String[] {"/actuator/health", "/actuator/health/liveness",
                "/actuator/health/readiness"}) {
            mvc.perform(get(address))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist())
                    .andExpect(jsonPath("$.details").doesNotExist());
        }
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        // Sem exposição, estes endereços não chegam ao Actuator: nenhum devolve configuração,
        // beans ou a lista de endpoints.
        for (String address : new String[] {"/actuator", "/actuator/env", "/actuator/beans",
                "/actuator/configprops", "/actuator/loggers"}) {
            mvc.perform(get(address))
                    .andExpect(content().string(not(containsString("propertySources"))))
                    .andExpect(content().string(not(containsString("\"_links\""))))
                    .andExpect(content().string(not(containsString("\"contexts\""))))
                    .andExpect(content().string(not(containsString("\"levels\""))));
        }
    }
}
