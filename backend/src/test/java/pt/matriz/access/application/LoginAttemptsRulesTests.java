package pt.matriz.access.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários de {@link LoginAttempts} (passo 2b, DA37): com 5 falhas em 15 minutos, o par
 * email e endereço IP espera 15 minutos (SEG09, DA26). As falhas mais antigas do que a janela não
 * contam; entrar com sucesso e repor o acesso limpam as falhas (DA25).
 */
class LoginAttemptsRulesTests {

    private static final String PEDRO_HERE = LoginAttempts.key("pedro@empresa.example", "10.0.0.7");
    private static final String PEDRO_ELSEWHERE = LoginAttempts.key("pedro@empresa.example", "10.0.0.8");
    private static final String MARTA_HERE = LoginAttempts.key("marta@empresa.example", "10.0.0.7");

    private MovableClock clock;
    private LoginAttempts attempts;

    @BeforeEach
    void setUp() {
        clock = new MovableClock(Instant.parse("2026-11-10T10:00:00Z"));
        attempts = new LoginAttempts(clock);
    }

    @Test
    void keyJoinsTheEmailAndTheAddress() {
        assertThat(PEDRO_HERE).isEqualTo("pedro@empresa.example|10.0.0.7");
    }

    @Test
    void fourFailuresDoNotBlockAndTheFifthDoes() {
        fail(PEDRO_HERE, 4);
        assertThat(attempts.isBlocked(PEDRO_HERE)).isFalse();
        fail(PEDRO_HERE, 1);
        assertThat(attempts.isBlocked(PEDRO_HERE)).isTrue();
    }

    @Test
    void blockOnlyAffectsThatEmailAndAddress() {
        fail(PEDRO_HERE, 5);
        assertThat(attempts.isBlocked(PEDRO_ELSEWHERE)).isFalse();
        assertThat(attempts.isBlocked(MARTA_HERE)).isFalse();
    }

    @Test
    void blockEndsAfter15Minutes() {
        fail(PEDRO_HERE, 5);
        clock.advance(Duration.ofMinutes(14).plusSeconds(59));
        assertThat(attempts.isBlocked(PEDRO_HERE)).isTrue();
        clock.advance(Duration.ofSeconds(1));
        assertThat(attempts.isBlocked(PEDRO_HERE)).isFalse();
        // Depois do bloqueio, as falhas começam do zero.
        fail(PEDRO_HERE, 4);
        assertThat(attempts.isBlocked(PEDRO_HERE)).isFalse();
    }

    @Test
    void failuresOlderThan15MinutesDoNotCount() {
        fail(PEDRO_HERE, 4);
        clock.advance(Duration.ofMinutes(15));
        fail(PEDRO_HERE, 4);
        assertThat(attempts.isBlocked(PEDRO_HERE)).isFalse();
        fail(PEDRO_HERE, 1);
        assertThat(attempts.isBlocked(PEDRO_HERE)).isTrue();
    }

    @Test
    void successfulEntryClearsTheFailures() {
        fail(PEDRO_HERE, 4);
        attempts.reset(PEDRO_HERE);
        fail(PEDRO_HERE, 4);
        assertThat(attempts.isBlocked(PEDRO_HERE)).isFalse();
    }

    @Test
    void resetAccessClearsFailuresAndBlocksOfThatEmailInAllAddresses() {
        fail(PEDRO_HERE, 5);
        fail(PEDRO_ELSEWHERE, 4);
        fail(MARTA_HERE, 5);

        attempts.clearEmail("pedro@empresa.example");

        assertThat(attempts.isBlocked(PEDRO_HERE)).isFalse();
        fail(PEDRO_ELSEWHERE, 1);
        assertThat(attempts.isBlocked(PEDRO_ELSEWHERE)).isFalse();
        assertThat(attempts.isBlocked(MARTA_HERE)).isTrue();
    }

    private void fail(String key, int times) {
        for (int i = 0; i < times; i++) {
            attempts.recordFailure(key);
        }
    }

    /** Relógio que se pode avançar, para testar a janela de 15 minutos sem esperar. */
    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
