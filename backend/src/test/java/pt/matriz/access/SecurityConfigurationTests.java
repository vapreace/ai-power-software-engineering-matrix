package pt.matriz.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;

import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Configuração de segurança da Ficha PL 05 (docs/quality-security-review.md):
 * <ul>
 * <li>CT41: Content Security Policy e os outros cabeçalhos de segurança no frontend e na API. Os pedidos
 * são HTTP reais, porque a página inicial (/) é servida por reencaminhamento para o {@code index.html},
 * que o MockMvc não executa; foi aí que os cabeçalhos faltavam (verification.md, secção 15).</li>
 * <li>CT42: nenhum utilizador em memória criado pelo Spring Boot, cuja palavra-passe aparecia nos logs
 * (F14).</li>
 * </ul>
 * O {@code index.html} é o de teste, em {@code src/test/resources/static}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class SecurityConfigurationTests {

    private static final String POLICY = "default-src 'self'; img-src 'self' data:; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ApplicationContext context;

    @Test
    void frontendAndApiResponsesCarryTheSecurityHeaders() throws Exception {
        for (String method : new String[] {"GET", "HEAD"}) {
            for (String address : new String[] {"/", "/projetos/1", "/api/session"}) {
                HttpResponse<String> response = http.send(HttpRequest.newBuilder(
                                URI.create("http://127.0.0.1:" + port + address))
                        .method(method, HttpRequest.BodyPublishers.noBody()).build(),
                        HttpResponse.BodyHandlers.ofString());
                String request = method + " " + address;
                assertThat(response.headers().firstValue("Content-Security-Policy")).as(request).hasValue(POLICY);
                assertThat(response.headers().firstValue("X-Content-Type-Options")).as(request).hasValue("nosniff");
                assertThat(response.headers().firstValue("X-Frame-Options")).as(request).hasValue("DENY");
            }
        }
    }

    @Test
    void noInMemoryUserIsCreated() {
        assertThat(context.getBeanNamesForType(UserDetailsService.class)).isEmpty();
    }
}
