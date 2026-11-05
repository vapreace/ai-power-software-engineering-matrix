/**
 * Módulo Projetos. Projetos, gestor, estado, pesquisa e eliminação (RF07, RF08, RF40, RF41, RN05 a RN10, RN21, RN22, RN27, RN31, RN34).
 * <p>
 * As dependências permitidas seguem docs/architecture.md, secção 6. O teste
 * {@code ModularityTests} falha se o módulo usar outro módulo fora desta lista,
 * ou os subpacotes internos de outro módulo.
 */
@ApplicationModule(displayName = "Projetos", allowedDependencies = {"teams", "accounts", "common"})
package pt.matriz.projects;

import org.springframework.modulith.ApplicationModule;
