package pt.matriz.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

import pt.matriz.ApiTestSupport;
import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Regressão de F5 (docs/quality-security-review.md, CT38): dois Administradores ativos desativam-se
 * um ao outro ao mesmo tempo. Só um pedido pode passar e fica sempre um Administrador ativo (RN18).
 * O outro é recusado com 409 {@code LAST_ADMINISTRATOR} ou, se a conta de quem o fez já estiver
 * desativada quando o pedido é lido, com 401 (DA35). Repete-se várias vezes, porque a corrida não
 * acontece em todas as execuções.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class LastAdministratorConcurrencyTests extends ApiTestSupport {

    private static final int ROUNDS = 10;

    LastAdministratorConcurrencyTests() {
        super("10.9.0.38");
    }

    @Test
    void twoAdministratorsDeactivatingEachOtherAtTheSameTimeLeaveOneActive() throws Exception {
        user("ana", "Ana", "ADMINISTRATOR");
        user("bruno", "Bruno", "ADMINISTRATOR");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                jdbc.update("UPDATE users SET status = 'ACTIVE' WHERE role = 'ADMINISTRATOR'");
                MockHttpSession ana = login("ana");
                MockHttpSession bruno = login("bruno");

                CountDownLatch start = new CountDownLatch(1);
                Future<Integer> first = pool.submit(() -> deactivate(start, ana, "bruno"));
                Future<Integer> second = pool.submit(() -> deactivate(start, bruno, "ana"));
                start.countDown();

                List<Integer> codes = new ArrayList<>(List.of(first.get(30, TimeUnit.SECONDS),
                        second.get(30, TimeUnit.SECONDS)));
                codes.sort(null);
                assertThat(codes.get(0)).as("ronda %d: %s", round, codes).isEqualTo(200);
                assertThat(codes.get(1)).as("ronda %d: %s", round, codes).isIn(401, 409);
                assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM users WHERE role = 'ADMINISTRATOR' AND status = 'ACTIVE'", Long.class))
                        .as("Administradores ativos na ronda %d", round).isEqualTo(1L);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private int deactivate(CountDownLatch start, MockHttpSession session, String person) throws Exception {
        start.await();
        return mvc.perform(put("/api/users/{id}/status", ids.get(person)).session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"DEACTIVATED\"}"))
                .andReturn().getResponse().getStatus();
    }
}
