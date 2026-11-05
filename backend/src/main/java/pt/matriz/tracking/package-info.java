/**
 * Módulo Acompanhamento. Só leitura: lista de projetos com progresso e atrasos, painel inicial (RF09, RF27, RF28, RF29, RN33).
 * <p>
 * As dependências permitidas seguem docs/architecture.md, secção 6. O teste
 * {@code ModularityTests} falha se o módulo usar outro módulo fora desta lista,
 * ou os subpacotes internos de outro módulo.
 */
@ApplicationModule(displayName = "Acompanhamento", allowedDependencies = {"projects", "tasks", "common"})
package pt.matriz.tracking;

import org.springframework.modulith.ApplicationModule;
