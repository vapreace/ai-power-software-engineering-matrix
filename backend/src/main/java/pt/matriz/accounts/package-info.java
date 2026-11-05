/**
 * Módulo Contas. Contas, papéis e estado da conta; garante um Administrador ativo (RF02, RN04, RN16, RN18, RN23, RN28).
 * <p>
 * As dependências permitidas seguem docs/architecture.md, secção 6. O teste
 * {@code ModularityTests} falha se o módulo usar outro módulo fora desta lista,
 * ou os subpacotes internos de outro módulo.
 */
@ApplicationModule(displayName = "Contas", allowedDependencies = {"common"})
package pt.matriz.accounts;

import org.springframework.modulith.ApplicationModule;
