import { defineConfig, devices } from '@playwright/test'

// Testes E2E (DA37, docs/test-plan.md): percursos do utilizador no browser, contra a aplicação
// a correr com o perfil demo. O backend e a base de dados têm de estar ligados (README.md).
// Sem MATRIZ_E2E_BASE_URL, usa o servidor do Vite em http://localhost:5173 e arranca-o se
// ainda não estiver a correr. Com o .jar de entrega: MATRIZ_E2E_BASE_URL=http://localhost:8080.
const baseURL = process.env.MATRIZ_E2E_BASE_URL ?? 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  // Um teste de cada vez: todos usam a mesma base de dados de demonstração.
  workers: 1,
  fullyParallel: false,
  retries: 0,
  forbidOnly: !!process.env.CI,
  // Resultados: na consola, no relatório HTML (playwright-report/) e em test-results/results.json.
  reporter: [['list'], ['html', { open: 'never' }], ['json', { outputFile: 'test-results/results.json' }]],
  use: {
    baseURL,
    // Traces e screenshots só quando um teste falha, para diagnosticar.
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env.MATRIZ_E2E_BASE_URL
    ? undefined
    : { command: 'npm run dev', url: baseURL, reuseExistingServer: true },
})
