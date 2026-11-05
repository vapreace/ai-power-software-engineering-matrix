package pt.matriz.accounts.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Palavras-passe guardadas só como hash bcrypt (SEG03, DA26). */
@Configuration(proxyBeanMethods = false)
class PasswordEncoderConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
