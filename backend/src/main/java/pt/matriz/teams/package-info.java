/**
 * Módulo Equipas. Equipas e pertença a equipas (RF05, RF06, RN02, RN03, RN17, RN23, RN26).
 * <p>
 * As dependências permitidas seguem docs/architecture.md, secção 6. O teste
 * {@code ModularityTests} falha se o módulo usar outro módulo fora desta lista,
 * ou os subpacotes internos de outro módulo.
 */
@ApplicationModule(displayName = "Equipas", allowedDependencies = {"accounts", "common"})
package pt.matriz.teams;

import org.springframework.modulith.ApplicationModule;
