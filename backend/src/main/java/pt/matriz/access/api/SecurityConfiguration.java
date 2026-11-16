package pt.matriz.access.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.servlet.HandlerExceptionResolver;

import pt.matriz.access.application.CurrentUsers;
import pt.matriz.access.application.SessionService;
import pt.matriz.accounts.Accounts;
import pt.matriz.common.ApiException;
import pt.matriz.common.ProblemCode;

/**
 * Autenticação com sessão no servidor e cookie HttpOnly, com proteção CSRF (DA04, DA26).
 * Todos os pedidos a /api exigem sessão, exceto entrar (SEG01). As respostas de erro
 * seguem o formato Problem Details (DA16).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
class SecurityConfiguration {

    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; img-src 'self' data:; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contextRepository,
            Accounts accounts, CurrentUsers currentUsers,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        http
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/session").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) -> exceptionResolver.resolveException(
                                request, response, null,
                                new ApiException(HttpStatus.UNAUTHORIZED, ProblemCode.UNAUTHENTICATED,
                                        SessionService.SESSION_ENDED)))
                        .accessDeniedHandler((request, response, ex) -> exceptionResolver.resolveException(
                                request, response, null,
                                new ApiException(HttpStatus.FORBIDDEN, ProblemCode.FORBIDDEN,
                                        "Não tem permissão para esta operação."))))
                // O frontend só carrega recursos da própria origem (DA02). Os outros cabeçalhos de
                // segurança do Spring Security mantêm-se; o HSTS só sai em pedidos HTTPS.
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        // Cabeçalhos escritos logo no início do pedido. Sem isto, a página inicial (/),
                        // que o Spring Boot serve reencaminhando para o index.html, saía sem eles: a
                        // resposta do reencaminhamento fecha sem passar pelo filtro que os acrescenta.
                        .addObjectPostProcessor(new ObjectPostProcessor<HeaderWriterFilter>() {
                            @Override
                            public <O extends HeaderWriterFilter> O postProcess(O filter) {
                                filter.setShouldWriteHeadersEagerly(true);
                                return filter;
                            }
                        }))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .addFilterBefore(new CurrentAccountFilter(accounts, currentUsers, exceptionResolver),
                        AuthorizationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
