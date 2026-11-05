package pt.matriz.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

/**
 * Relógio da organização (RNF06, DA24). Todas as regras de datas usam a data atual
 * daqui, nunca diretamente a do sistema. Nos testes, o {@link Clock} é substituído
 * por um relógio fixo (2026-11-10).
 */
@Component
public class OrganizationClock {

    private final Clock clock;

    public OrganizationClock(Clock clock) {
        this.clock = clock;
    }

    /** Data atual no fuso da organização (requirements.md, secção 2). */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Data e hora atuais no fuso da organização (por exemplo, para os comentários, RN30). */
    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    public ZoneId zone() {
        return clock.getZone();
    }
}
