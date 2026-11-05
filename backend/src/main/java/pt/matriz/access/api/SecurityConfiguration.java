package pt.matriz.access.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .addFilterBefore(new CurrentAccountFilter(accounts, currentUsers, exceptionResolver),
                        AuthorizationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
