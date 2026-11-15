package pt.matriz;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Relógio da organização com data fixa nos testes: 2026-11-10, a data atual das histórias
 * (user-stories.md), no fuso de DA24 (RNF06, DA31).
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

    public static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");

    @Bean
    @Primary
    Clock fixedOrganizationClock() {
        return Clock.fixed(LocalDateTime.of(2026, 11, 10, 10, 0).atZone(ZONE).toInstant(), ZONE);
    }
}
