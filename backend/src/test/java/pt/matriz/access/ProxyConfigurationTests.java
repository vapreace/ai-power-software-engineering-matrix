package pt.matriz.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import pt.matriz.ApiTestSupport;
import pt.matriz.FixedClockConfiguration;
import pt.matriz.TestcontainersConfiguration;

/**
 * Configuração do ambiente publicado atrás do proxy do Render (DA40), com pedidos HTTP reais ao
 * servidor: o proxy termina o HTTPS e envia {@code X-Forwarded-For} e {@code X-Forwarded-Proto}.
 * <ul>
 * <li>F6 (CT39): o cookie de sessão sai com {@code Secure}, e o HSTS e o cookie CSRF também, porque
 * a aplicação sabe que o pedido chegou por HTTPS.</li>
 * <li>F13 (CT40): o limite de tentativas (DA26) conta pelo endereço de quem faz o pedido, e não pelo
 * do proxy, igual para todos.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"MATRIZ_COOKIE_SECURE=true", "MATRIZ_FORWARD_HEADERS=native"})
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
class ProxyConfigurationTests extends ApiTestSupport {

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    ProxyConfigurationTests() {
        super("10.9.0.40");
    }

    @BeforeEach
    void setUp() {
        user("rui", "Rui", "MANAGER");
    }

    @Test
    void behindTheHttpsProxyTheCookiesAreSecureAndHstsIsSent() throws Exception {
        HttpResponse<String> anonymous = send("GET", "203.0.113.30", null, null);
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(anonymous.headers().firstValue("Strict-Transport-Security")).isPresent();
        assertThat(cookie(anonymous, "XSRF-TOKEN")).contains("; Secure");

        HttpResponse<String> login = send("POST", "203.0.113.30", token(anonymous),
                "{\"email\": \"rui@empresa.example\", \"password\": \"" + PASSWORD + "\"}");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(cookie(login, "JSESSIONID")).contains("; Secure").contains("; HttpOnly").contains("SameSite=Lax");
    }

    @Test
    void loginAttemptsAreCountedPerClientAddressAndNotPerProxy() throws Exception {
        String wrong = "{\"email\": \"rui@empresa.example\", \"password\": \"errada-errada-1\"}";
        String right = "{\"email\": \"rui@empresa.example\", \"password\": \"" + PASSWORD + "\"}";
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(attempt("203.0.113.10", wrong).statusCode()).as("falha %d", attempt).isEqualTo(401);
        }
        // O endereço das cinco falhas fica a aguardar, mesmo com a palavra-passe certa (DA26).
        assertThat(attempt("203.0.113.10", right).statusCode()).isEqualTo(429);
        // Outra pessoa, atrás do mesmo proxy, entra: o contador não é partilhado.
        assertThat(attempt("203.0.113.20", right).statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> attempt(String clientAddress, String body) throws Exception {
        return send("POST", clientAddress, token(send("GET", clientAddress, null, null)), body);
    }

    /** Pedido a /api/session como chegaria do proxy: HTTPS terminado no proxy e endereço do cliente. */
    private HttpResponse<String> send(String method, String clientAddress, String csrfToken, String body)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/session"))
                .header("X-Forwarded-For", clientAddress)
                .header("X-Forwarded-Proto", "https");
        if (csrfToken != null) {
            request.header("Cookie", "XSRF-TOKEN=" + csrfToken).header("X-XSRF-TOKEN", csrfToken);
        }
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String cookie(HttpResponse<String> response, String name) {
        List<String> cookies = response.headers().allValues("Set-Cookie");
        return cookies.stream().filter(value -> value.startsWith(name + "=")).findFirst()
                .orElseThrow(() -> new AssertionError("Sem o cookie " + name + ": " + cookies));
    }

    private static String token(HttpResponse<String> response) {
        String value = cookie(response, "XSRF-TOKEN");
        return value.substring("XSRF-TOKEN=".length(), value.indexOf(';'));
    }
}
