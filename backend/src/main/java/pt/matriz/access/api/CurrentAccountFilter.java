package pt.matriz.access.api;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import pt.matriz.access.application.CurrentUsers;
import pt.matriz.accounts.AccountStatus;
import pt.matriz.accounts.AccountView;
import pt.matriz.accounts.Accounts;
import pt.matriz.common.ApiException;
import pt.matriz.common.CurrentUser;
import pt.matriz.common.ProblemCode;

/**
 * Em cada pedido com sessão, lê a conta da base de dados (architecture.md, secção 5.2).
 * <ul>
 * <li>Conta desativada ou inexistente: a sessão termina e o pedido segue sem autenticação,
 * por isso recebe 401 (SEG08, DA35).</li>
 * <li>Palavra-passe temporária: só deixa entrar, ver a sessão, sair e definir a palavra-passe nova;
 * o resto recebe 403 {@code PASSWORD_CHANGE_REQUIRED} (DA25).</li>
 * </ul>
 * Mudar o papel ou as equipas tem efeito no pedido seguinte (E39).
 */
class CurrentAccountFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_WITH_TEMPORARY_PASSWORD = Set.of(
            "POST /api/session", "GET /api/session", "DELETE /api/session", "PUT /api/session/password");

    private final Accounts accounts;
    private final CurrentUsers currentUsers;
    private final HandlerExceptionResolver exceptionResolver;

    CurrentAccountFilter(Accounts accounts, CurrentUsers currentUsers, HandlerExceptionResolver exceptionResolver) {
        this.accounts = accounts;
        this.currentUsers = currentUsers;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long accountId) {
            Optional<AccountView> account = accounts.findById(accountId)
                    .filter(found -> found.status() == AccountStatus.ACTIVE);
            if (account.isEmpty()) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();
            } else {
                CurrentUser user = currentUsers.from(account.get());
                request.setAttribute(CurrentUsers.REQUEST_ATTRIBUTE, user);
                String path = request.getRequestURI().substring(request.getContextPath().length());
                if (user.mustChangePassword() && path.startsWith("/api/")
                        && !ALLOWED_WITH_TEMPORARY_PASSWORD.contains(request.getMethod() + " " + path)) {
                    exceptionResolver.resolveException(request, response, null, new ApiException(
                            HttpStatus.FORBIDDEN, ProblemCode.PASSWORD_CHANGE_REQUIRED,
                            "Defina uma palavra-passe nova para continuar."));
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
