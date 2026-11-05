/**
 * Módulo Acesso. Entrar e sair, sessões, palavra-passe temporária e limite de tentativas (RF01, SEG01, SEG03 a SEG09, DA25, DA26, DA35).
 * <p>
 * As dependências permitidas seguem docs/architecture.md, secção 6. O teste
 * {@code ModularityTests} falha se o módulo usar outro módulo fora desta lista,
 * ou os subpacotes internos de outro módulo.
 */
@ApplicationModule(displayName = "Acesso", allowedDependencies = {"accounts", "teams", "common"})
package pt.matriz.access;

import org.springframework.modulith.ApplicationModule;
