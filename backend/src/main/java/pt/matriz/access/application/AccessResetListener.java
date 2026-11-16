package pt.matriz.access.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import pt.matriz.accounts.AccessReset;

/** Repor o acesso limpa as tentativas falhadas desse email (US02, DA25, SEG09 c). */
@Component
class AccessResetListener {

    private final LoginAttempts attempts;

    AccessResetListener(LoginAttempts attempts) {
        this.attempts = attempts;
    }

    @EventListener
    void on(AccessReset event) {
        attempts.clearEmail(event.email());
    }
}
