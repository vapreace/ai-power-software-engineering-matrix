// Análise estática do frontend (DA37, Ficha PL 04, exercício 5). Configuração base do create-vue:
// regras essenciais do Vue, regras recomendadas de TypeScript e regras do Playwright nos testes E2E.
// Sem regras só de estilo: o objetivo são erros e práticas inseguras.
import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginPlaywright from 'eslint-plugin-playwright'

export default defineConfigWithVueTs(
  { name: 'app/files-to-lint', files: ['**/*.{ts,mts,tsx,vue}'] },
  globalIgnores(['**/dist/**', '**/coverage/**', 'playwright-report/**', 'test-results/**']),
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  {
    name: 'app/security',
    rules: {
      // HTML vindo da API nunca é inserido sem escape (XSS, OWASP A03).
      'vue/no-v-html': 'error',
    },
  },
  { ...pluginPlaywright.configs['flat/recommended'], files: ['e2e/**/*.spec.ts'] },
)
