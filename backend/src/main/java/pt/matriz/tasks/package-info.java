/**
 * Módulo Tarefas. Tarefas, responsáveis, comentários e "As minhas tarefas" (RF12 a RF17, RF19, RN11 a RN17, RN19, RN24, RN25, RN30).
 * <p>
 * As dependências permitidas seguem docs/architecture.md, secção 6. O teste
 * {@code ModularityTests} falha se o módulo usar outro módulo fora desta lista,
 * ou os subpacotes internos de outro módulo.
 */
@ApplicationModule(displayName = "Tarefas", allowedDependencies = {"projects", "teams", "accounts", "common"})
package pt.matriz.tasks;

import org.springframework.modulith.ApplicationModule;
