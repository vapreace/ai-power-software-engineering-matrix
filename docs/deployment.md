# Ambiente publicado da Matriz

> **Versão:** 0.1 (Ficha PL 05, passo 6)
> **Estado:** em funcionamento desde 16/11/2026. Decisões em [architecture.md](architecture.md): DA38 (imagem), DA39 (pipeline) e DA40 (ambientes e alojamento).

Documentos relacionados:

- [architecture.md](architecture.md#41-implantação): diagramas da execução local e do ambiente publicado.
- [../render.yaml](../render.yaml): definição do serviço no Render.
- [verification.md](verification.md), secção 15.6: verificações feitas no ambiente publicado.
- [troubleshooting.md](troubleshooting.md): diagnóstico de problemas.

---

## 1. O ambiente

| Item | Valor |
|------|-------|
| Endereço | https://matriz-3lgd.onrender.com |
| Finalidade | Demonstração da versão candidata (DA40). Não é produção nem staging |
| Aplicação | Render, Web Service com a imagem do `Dockerfile` (DA38), plano gratuito, região de Frankfurt, serviço `matriz` criado a partir de [render.yaml](../render.yaml) (Blueprint `matriz`) |
| Base de dados | Neon, PostgreSQL 18, plano gratuito, região AWS Europe Central 1 (Frankfurt), projeto `matriz`, ramo `production`, base `neondb`, ligação direta (sem pooling) e com TLS |
| Dados | Perfil `demo`: as contas e os projetos de demonstração do [README](../README.md#contas-de-demonstração), fictícios (PRIV03) |
| Entrega | Automática: o Render faz deploy de `main` só depois de todos os checks da pipeline passarem (`autoDeployTrigger: checksPass`) |

As contas de demonstração e a palavra-passe são públicas (README). Qualquer pessoa com o endereço pode entrar e alterar dados: é um risco aceite, porque os dados são fictícios (DA40). A secção 5.3 diz como repor a base de dados.

**Exceção: a conta de Administrador (Ana).** No ambiente publicado tem uma palavra-passe só da equipa (F16): com a palavra-passe pública, qualquer pessoa podia repor o acesso das outras contas ou desativá-las e bloquear uma demonstração. Mudou-se pela própria aplicação: a Ana entrou, usou **Repor acesso** na sua conta com uma palavra-passe temporária e definiu uma nova na entrada seguinte (DA25). A palavra-passe não fica escrita em nenhum documento; quem avalia o projeto e precisar do Administrador recebe-a em privado. Depois de repor a base de dados (5.3), a Ana volta à palavra-passe de demonstração e o passo repete-se.

---

## 2. Configuração

Os valores sem segredo estão em [render.yaml](../render.yaml). Os quatro da base de dados têm `sync: false`: foram introduzidos uma vez no Render, a partir da janela **Connect** do Neon, e ficam só no painel do Render (separador **Environment** do serviço). Nunca entram no Git nem no chat.

| Variável | Valor | Para quê |
|----------|-------|----------|
| `PORT` | `8080` | Porta em que o Render encaminha os pedidos (a da aplicação). |
| `SPRING_PROFILES_ACTIVE` | `demo` | Dados de demonstração (DA30). |
| `MATRIZ_DB_HOST`, `MATRIZ_DB_NAME`, `MATRIZ_DB_USER`, `MATRIZ_DB_PASSWORD` | Painel do Render (`sync: false`) | Ligação ao Neon. O host é o da ligação direta, sem `-pooler`. |
| `MATRIZ_DB_PORT` | `5432` | |
| `MATRIZ_DB_SSLMODE` | `require` | O Neon exige TLS. |
| `MATRIZ_COOKIE_SECURE` | `true` | Cookie de sessão só por HTTPS (F6). |
| `MATRIZ_FORWARD_HEADERS` | `native` | O endereço de quem faz o pedido e o HTTPS vêm dos cabeçalhos `X-Forwarded-*` do proxy do Render (F13). Com isto, o HSTS e o cookie CSRF também saem com `Secure`. |
| `LOGGING_STRUCTURED_FORMAT_CONSOLE` | `ecs` | Logs em JSON (DA38). |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -XX:+UseSerialGC -XX:TieredStopAtLevel=1` | 512 MB e uma fração de CPU: GC simples e arranque mais rápido. |

Mudar uma variável no painel do Render reinicia o serviço. Mudar o `render.yaml` no `main` também atualiza o serviço (sincronização do Blueprint).

---

## 3. Como foi criado

Para recriar o ambiente numa conta nova:

1. **Neon:** conta com o login do GitHub; **New project** com o nome `matriz`, Postgres 18, AWS, região Europe Central 1 (Frankfurt). Em **Connect**, desligar **Connection pooling** e anotar o host, a base, o utilizador e a palavra-passe.
2. **Render:** conta com o login do GitHub, com acesso só a este repositório; **New**, **Blueprint**, escolher o repositório. O Render lê o [render.yaml](../render.yaml) e pede os quatro valores da base de dados. **Deploy Blueprint**.
3. **GitHub:** variável do repositório `MATRIZ_PUBLIC_URL` com o endereço do serviço (**Settings**, **Secrets and variables**, **Actions**, **Variables**), para o workflow de validação.

No primeiro arranque, o Liquibase cria o esquema e os dados de demonstração no Neon (DA30), como com o Docker Compose.

O assistente de entrada do Neon propõe prompts para um agente de IA instalar a CLI do Neon, escrever as credenciais no `.env` e mudar o código da aplicação. Não foram usados: o `.env` aponta para o PostgreSQL local, as credenciais vão só para o Render e a ligação à base de dados já existe (DA30). O mesmo assistente criou um primeiro projeto em Londres, que foi apagado e substituído pelo projeto em Frankfurt.

---

## 4. Entrega contínua e validação

```
push para main  ->  pipeline (DA39)  ->  todos os checks aprovados?  -> sim ->  Render faz o build da imagem e o deploy
                                                                     -> não ->  nada muda no ambiente publicado
```

- O Render constrói a imagem a partir do `Dockerfile` e só passa os pedidos para a versão nova quando a readiness responde `UP` (health check `/actuator/health/readiness`, que inclui a base de dados).
- **Validação a pedido:** em **Actions**, **Validate published**, **Run workflow** (`.github/workflows/validate-published.yml`). Acorda a aplicação e verifica a readiness, a página inicial, a API sem sessão (401), a CSP, o HSTS e o cookie CSRF com `Secure`. Não faz deploy.
- **Deploy manual:** no Render, no serviço `matriz`, **Manual Deploy**, **Deploy latest commit**.

---

## 5. Operação

### 5.1 Limites do plano gratuito

| Limite | Consequência | O que fazer |
|--------|--------------|-------------|
| O serviço adormece após 15 minutos sem pedidos e demora cerca de um minuto a acordar (LIM17) | O primeiro pedido fica à espera; as sessões e os contadores de tentativas perdem-se (DA26) | Abrir o endereço uns minutos antes de uma demonstração e voltar a entrar |
| 512 MB de memória e uma fração de CPU | O arranque é mais lento do que localmente | As opções da JVM da secção 2; o Compose usa o mesmo limite de memória para detetar problemas cá (DA38) |
| 750 horas de instância por mês, por workspace | Chega para um serviço sempre ligado | |
| O Neon suspende a base de dados após 5 minutos sem consultas | A primeira consulta seguinte demora um pouco mais | Nada: o pool de ligações volta a ligar-se sozinho |

### 5.2 Logs

No Render, no serviço `matriz`, separador **Logs**. Os logs estão em JSON (formato ECS). Os logs não mostram a palavra-passe da base de dados.

### 5.3 Repor a base de dados de demonstração

Procedimento previsto, ainda não executado:

1. No Neon, no projeto `matriz`, abrir o **SQL Editor** e correr `DROP SCHEMA public CASCADE; CREATE SCHEMA public;`. Apaga todos os dados do ambiente publicado.
2. No Render, no serviço `matriz`, **Manual Deploy**, **Restart service**. No arranque, o Liquibase cria de novo o esquema e os dados de demonstração.
3. Repetir a mudança da palavra-passe da Ana (secção 1).
4. Correr o workflow **Validate published**.

### 5.4 Voltar a uma versão anterior

No Render, em **Events** ou **Deploys**, escolher um deploy anterior e usar **Rollback**. Os changesets do Liquibase só andam para a frente: uma versão anterior só serve se não tiver havido changesets novos depois dela (DA40). Com changesets novos, repõe-se a base de dados (5.3) ou faz-se uma correção para a frente.

---

## 6. Segurança

| Medida | Onde |
|--------|------|
| Segredos só no painel do Render, introduzidos pela pessoa; nunca no Git, na pipeline ou no chat | Secção 2 |
| Conta de Administrador com palavra-passe só da equipa | Secção 1, F16 |
| HTTPS do Render, com HSTS | F13, DA40 |
| Cookie de sessão `Secure`, `HttpOnly` e `SameSite=Lax` | F6 ([verification/render-cookies.png](verification/render-cookies.png)) |
| TLS entre a aplicação e o Neon | `MATRIZ_DB_SSLMODE=require` |
| Dados na UE (Frankfurt), fictícios | PRIV02, PRIV03 |
| Render com acesso só a este repositório | Secção 3 |
