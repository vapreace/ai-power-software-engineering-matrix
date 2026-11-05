package pt.matriz.access.api;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.matriz.access.application.SessionService;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.CurrentUserProvider;

/** Operações de sessão de openapi.yaml (US01). */
@RestController
@RequestMapping("/api/session")
class SessionController {

    private final SessionService sessions;
    private final CurrentUserProvider currentUser;
    private final SecurityContextRepository contextRepository;

    SessionController(SessionService sessions, CurrentUserProvider currentUser,
            SecurityContextRepository contextRepository) {
        this.sessions = sessions;
        this.currentUser = currentUser;
        this.contextRepository = contextRepository;
    }

    /** Entrar. A sessão guarda só o identificador da conta (architecture.md, secção 5.2). */
    @PostMapping
    CurrentUser login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        CurrentUser user = sessions.login(body.email(), body.password(), request.getRemoteAddr());

        // O identificador de sessão muda ao entrar (DA26).
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            request.changeSessionId();
        } else {
            request.getSession(true);
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user.id(), null, List.of()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return user;
    }

    @GetMapping
    CurrentUser current() {
        return currentUser.get();
    }

    /** Sair: a sessão termina no servidor (SEG07). */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setNewPassword(@RequestBody SetPasswordRequest body) {
        sessions.changeTemporaryPassword(currentUser.get(), body.newPassword());
    }
}
