package pt.matriz;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Teste de arquitetura (DA28, architecture.md secção 12.2): não há ciclos entre
 * módulos, cada módulo só usa os módulos permitidos no seu package-info.java e
 * ninguém usa os subpacotes internos de outro módulo.
 */
class ModularityTests {

    @Test
    void modulesRespectTheArchitecture() {
        ApplicationModules.of(MatrizApplication.class).verify();
    }
}
