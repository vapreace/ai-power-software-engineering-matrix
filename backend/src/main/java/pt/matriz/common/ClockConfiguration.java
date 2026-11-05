package pt.matriz.common;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Fuso horário fixo da organização, definido na configuração (DA24). */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock organizationClockSource(@Value("${matriz.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
