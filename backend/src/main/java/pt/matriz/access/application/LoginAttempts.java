package pt.matriz.access.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Limite de tentativas de entrada (SEG09, DA26). As falhas contam-se por par email e
 * endereço IP, só em memória. Com 5 falhas em 15 minutos, esse par espera 15 minutos.
 * Reiniciar a aplicação limpa os contadores (LIM14).
 */
@Component
class LoginAttempts {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new HashMap<>();
    private final Map<String, Instant> blockedUntil = new HashMap<>();

    LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    static String key(String normalizedEmail, String remoteAddress) {
        return normalizedEmail + "|" + remoteAddress;
    }

    synchronized boolean isBlocked(String key) {
        Instant until = blockedUntil.get(key);
        if (until == null) {
            return false;
        }
        if (clock.instant().isBefore(until)) {
            return true;
        }
        blockedUntil.remove(key);
        return false;
    }

    synchronized void recordFailure(String key) {
        Instant now = clock.instant();
        Deque<Instant> recent = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!recent.isEmpty() && !recent.peekFirst().isAfter(now.minus(WINDOW))) {
            recent.pollFirst();
        }
        recent.addLast(now);
        if (recent.size() >= MAX_FAILURES) {
            blockedUntil.put(key, now.plus(WINDOW));
            failures.remove(key);
        }
    }

    synchronized void reset(String key) {
        failures.remove(key);
    }
}
