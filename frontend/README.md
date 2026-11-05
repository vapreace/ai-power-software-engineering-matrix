# Frontend da Matriz

Vue 3, TypeScript, Vite e Vue Router (DA28). As instruções para instalar e arrancar estão no [README.md](../README.md) da raiz.

| Pasta | Conteúdo |
|-------|----------|
| `src/api` | Único sítio com o endereço da API (`/api`) e os pedidos `fetch` (DA34). |
| `src/router` | Rotas. |
| `src/views` | Ecrãs (docs/ui/README.md, secção 9). |

Em desenvolvimento, o Vite reencaminha `/api` para o backend em `http://localhost:8080`. O `npm run build` escreve em `backend/src/main/resources/static`, para o backend servir o frontend.
