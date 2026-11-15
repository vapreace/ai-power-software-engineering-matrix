package pt.matriz.common;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serve o frontend compilado que vai dentro do {@code .jar} (DA02, DA28). Os endereços da
 * aplicação (por exemplo {@code /projetos/1}) não são ficheiros: devolve o {@code index.html},
 * para o Vue Router mostrar o ecrã certo ao abrir um endereço ou ao recarregar a página.
 * Os pedidos a {@code /api} e os ficheiros que não existem (com extensão, como {@code .js})
 * nunca recebem o {@code index.html}: respondem 404.
 */
@Configuration(proxyBeanMethods = false)
class SpaFallbackConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        String lastSegment = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                        if (resourcePath.startsWith("api/") || resourcePath.equals("api") || lastSegment.contains(".")) {
                            return null;
                        }
                        Resource index = location.createRelative("index.html");
                        return index.exists() && index.isReadable() ? index : null;
                    }
                });
    }
}
