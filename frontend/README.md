# Frontend da Matriz

Vue 3, TypeScript, Vite e Vue Router (DA28). As instruções para instalar e arrancar estão no [README.md](../README.md) da raiz.

| Pasta | Conteúdo |
|-------|----------|
| `src/api` | Único sítio com o endereço da API (`/api`) e os pedidos `fetch` (DA34). |
| `src/router` | Rotas. |
| `src/views` | Ecrãs (docs/ui/README.md, secção 9). |
| `src/__tests__` | Testes de regressão com Vitest (DA31). |
| `e2e` | Testes E2E com Playwright (DA37, docs/test-plan.md). Comando e pré-requisitos no README.md da raiz. |

Em desenvolvimento, o Vite reencaminha `/api` para o backend em `http://localhost:8080`. O `npm run build` escreve em `backend/src/main/resources/static`, para o backend servir o frontend.
