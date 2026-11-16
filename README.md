# Matriz

Aplicação de gestão de projetos e tarefas para equipas, desenvolvida no curso **AI-Powered Software Engineering: From Mindset to Delivery**.

> Fase atual: **Ficha PL 05**, versão candidata **1.0.0-rc.1** (tags `v1.0.0-rc.1` e `ficha-pl05`): imagem Docker, Docker Compose, pipeline de integração contínua e ambiente publicado em **https://matriz-3lgd.onrender.com**. MVP completo (I0 a I7); testes e revisão de qualidade e segurança na Ficha PL 04 (tag `ficha-pl04`). Alterações por versão em [CHANGELOG.md](CHANGELOG.md), notas da versão em [RELEASE_NOTES.md](RELEASE_NOTES.md) e verificação em [docs/verification.md](docs/verification.md).

---

## Problema

Uma empresa precisa de uma aplicação onde as suas equipas possam organizar projetos, gerir tarefas e acompanhar o progresso do trabalho.

Hoje, a informação sobre o trabalho está dispersa por folhas de cálculo, emails e reuniões. Ninguém sabe ao certo quem está a fazer o quê nem o que está atrasado.

A primeira frase é a descrição do cliente. A segunda é um pressuposto ainda por confirmar (ver [constraints.md](docs/constraints.md#2-pressupostos-assumidos)).

## Objetivos principais

Só a descrição do cliente está confirmada. Os objetivos e os papéis de utilizador são interpretações da equipa, ainda por validar.

1. Cada pessoa sabe que tarefas tem atribuídas e com que prazo.
2. O gestor de um projeto vê, a qualquer momento, o progresso e as tarefas atrasadas.
3. A direção acompanha o estado geral dos projetos sem pedir relatórios.
4. As equipas piloto deixam de usar folhas de cálculo para acompanhar o trabalho.

## Utilizadores

| Papel | Em resumo |
|-------|-----------|
| Administrador | Gere contas, equipas e papéis. |
| Gestor | Cria projetos, distribui tarefas e acompanha o progresso. |
| Membro | Executa e atualiza as suas tarefas. |
| Observador | Consulta o estado de todos os projetos, sem alterar. |

## Funcionalidades previstas para o MVP

| Área | Funcionalidades |
|------|-----------------|
| Acesso | Entrar e sair. Permissões por papel. O Administrador cria, edita, desativa e reativa contas, muda papéis e dá ou repõe o acesso. |
| Equipas | Criar e editar equipas. Adicionar e remover pessoas. |
| Projetos | Criar, editar e eliminar projetos. Mudar estado e arquivar. Lista de projetos com filtro por estado e pesquisa pelo nome. |
| Tarefas | Criar, editar e apagar. Um ou vários responsáveis. Prazo e prioridade. Mudar estado. Comentários. Lista com filtros. "As minhas tarefas". |
| Progresso | Painel inicial com as contagens de tarefas por estado. Percentagem de tarefas concluídas por projeto. Destaque e contagem de tarefas atrasadas. |

### O que já funciona

| Área | Funciona | Incremento |
|------|----------|------------|
| Acesso | Entrar, sair, palavra-passe nova na primeira entrada, sessão terminada. Permissões por papel em cada pedido. | I1 |
| Contas | O Administrador cria, edita, muda o papel, desativa, reativa e repõe o acesso. | I7 |
| Equipas | O Administrador cria, muda o nome, adiciona e remove pessoas. Cada pessoa vê as suas equipas. | I7 |
| Projetos | Criar, editar, eliminar com confirmação, lista com pesquisa e filtro por estado, progresso e atrasos. | I2 |
| Estado e gestor | Concluir (com confirmação), reabrir, arquivar e desarquivar. O Administrador muda o gestor. | I7 |
| Tarefas | Criar, editar, apagar, mudar o estado, vários responsáveis, comentários, filtros por responsável, estado, prioridade e atraso. | I3, I4 |
| As minhas tarefas | As tarefas de cada pessoa nos projetos ativos, por prazo, com "Incluir concluídas". | I7 |
| Progresso | Painel inicial com as contagens por estado, por projeto e no total. | I5 |
| Regras automáticas | Quem é desativado, sai de uma equipa ou muda de papel deixa as tarefas abertas "Sem responsável" e os projetos que gere "Sem gestor". | I7 |

Fora do MVP: quadro, histórico, registo das ações do Administrador, pesquisa por texto nas tarefas, avisos, anexos, subtarefas, relatórios, integrações. A lista completa está em [requirements.md, secção 9](docs/requirements.md#9-fora-do-âmbito-inicial).

```
 Administrador        Gestor                     Membro                Gestor
 ─────────────        ──────                     ──────                ──────
 1. Cria equipa  ──►  2. Cria projeto      ──►  4. Abre "As     ──►  6. Vê progresso
    e contas          3. Cria e atribui          minhas tarefas"      e tarefas
                         tarefas                5. Atualiza estado    atrasadas
```

## Tecnologias

Decisões em [docs/architecture.md](docs/architecture.md), secção 9 (DA01 a DA41).

| Área | Tecnologias |
|------|-------------|
| Backend | Java 21, Spring Boot 4.1.2 (Web MVC, Security, Data JPA, Validation, Actuator), Spring Modulith 2.1.2, Liquibase, Maven Wrapper |
| Frontend | Vue 3.5, Vue Router 5, TypeScript 6, Vite 8 |
| Base de dados | PostgreSQL 18 |
| Testes | JUnit Jupiter e Mockito (unitários), Spring Boot Test e Testcontainers (integração), Playwright 1.65 (E2E), Vitest (regressão do frontend), JaCoCo 0.8.15 (cobertura), ESLint |
| Entrega | Docker e Docker Compose, GitHub Actions, Trivy 0.76.2, Dependabot |
| Ambiente publicado | Render (aplicação) e Neon (PostgreSQL), em Frankfurt |

## Ambiente publicado

A versão de `main` está em **https://matriz-3lgd.onrender.com**, com as [contas de demonstração](#contas-de-demonstração), exceto a de Administrador, que lá tem uma palavra-passe só da equipa (F16). O Render só publica uma versão depois de a pipeline de integração contínua passar (DA40). O plano gratuito adormece após 15 minutos sem pedidos: o primeiro pedido seguinte demora cerca de um minuto. Configuração, limites e operação em [docs/deployment.md](docs/deployment.md).

## Como executar

Os comandos são para o Git Bash e começam na raiz do repositório. A configuração local fica no `.env`, copiado de `.env.example` (mudar `MATRIZ_DB_PASSWORD`); o `.env` nunca entra no Git.

### Com Docker Compose (só precisa do Docker)

```bash
cp .env.example .env          # só da primeira vez; mudar MATRIZ_DB_PASSWORD
docker compose up -d --build  # constrói a imagem da aplicação e arranca-a com o PostgreSQL 18
docker compose ps             # "app" e "postgres" ficam "healthy" ao fim de cerca de um minuto
```

A aplicação fica em http://localhost:8080, com as contas de demonstração (perfil `demo`, definido por `MATRIZ_PROFILES` no `.env`). A saúde está em `/actuator/health/liveness` e `/actuator/health/readiness` (a readiness inclui a base de dados).

| Para quê | Comando |
|----------|---------|
| Ver os logs da aplicação (JSON, formato ECS) | `docker compose logs -f app` |
| Parar e remover os containers (os dados ficam no volume) | `docker compose down` |
| Parar e apagar também a base de dados | `docker compose down -v` |
| Construir de novo depois de mudar o código | `docker compose up -d --build` |
| Construir só a imagem | `docker build -t matriz:local .` |

Serviços, rede, volume, healthchecks e restrições de execução em [architecture.md](docs/architecture.md), DA38. Problemas frequentes em [docs/troubleshooting.md](docs/troubleshooting.md).

### Em desenvolvimento

Requisitos: Java 21, Docker e Node 22.22.2 ou mais recente na versão 22, ou 24.15 ou mais recente (como em `engines` no `frontend/package.json`).

```bash
# 1. Configuração local (só da primeira vez). Mudar MATRIZ_DB_PASSWORD no .env.
cp .env.example .env

# 2. Só a base de dados (PostgreSQL 18), para desenvolver com o mvnw e o Vite
docker compose up -d postgres

# 3. Backend com os dados de demonstração, em http://localhost:8080
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# 4. Frontend, noutro terminal, em http://localhost:5173
cd frontend
npm install
npm run dev
```

Desde a Ficha PL 05, o projeto do Compose chama-se `matriz`; uma base de dados criada antes fica num volume antigo, que se apaga com `docker compose -p ai-power-software-engineering-matrix down -v`.

### Testes e outras tarefas

| Tarefa | Comando | Onde |
|--------|---------|------|
| Todos os testes do backend: unitários, integração e arquitetura (precisam do Docker) | `./mvnw verify` | `backend/` |
| Só os testes unitários (não precisam do Docker) | `./mvnw test -Dtest='*RulesTests'` | `backend/` |
| Cobertura de código (JaCoCo): total no fim de `./mvnw verify`; só unitários com este comando. Relatório em `backend/target/site/jacoco/index.html` | `./mvnw clean test -Dtest='*RulesTests' jacoco:report` | `backend/` |
| Verificar os tipos do frontend | `npm run type-check` | `frontend/` |
| Testes de regressão do frontend | `npm run test:unit` | `frontend/` |
| Testes E2E (com a aplicação a correr, ver abaixo) | `npm run test:e2e` | `frontend/` |
| Análise estática do frontend (ESLint) | `npm run lint` | `frontend/` |
| Build para entregar | `npm run build` e depois `./mvnw package` | `frontend/`, depois `backend/` |
| Aplicação completa num container, com o PostgreSQL, em http://localhost:8080 (DA38) | `docker compose up -d --build` | raiz |
| Apagar a base de dados local | `docker compose down -v` | raiz |

Números da última verificação: 423 testes no backend (291 unitários, que cobrem todas as branches das regras de negócio, e 132 de integração e arquitetura), 4 testes de regressão no frontend e 8 testes E2E (4 percursos no computador e num ecrã de 360 px). Resultados em [docs/verification.md](docs/verification.md).

### Pipeline de integração contínua

Cada push e pull request para `main` corre `.github/workflows/ci.yml` no GitHub Actions (DA39), em quatro blocos. Cada bloco só começa quando todos os jobs do anterior passam:

| Bloco | Jobs |
|-------|------|
| Build | Frontend (tipos, ESLint, `npm audit`, build) e backend (compilação) |
| Testes | Unitários, integração (Testcontainers), frontend (Vitest) e E2E (o `.jar` com o perfil `demo`) |
| Cobertura | Relatórios JaCoCo; chumba se uma branch das regras de negócio ficar sem teste unitário |
| Entrega | Imagem (Compose, smoke tests e Trivy) e, só numa release, publicação da imagem no GitHub Container Registry |

Os resultados ficam no separador **Actions** do repositório, com os relatórios dos testes, da cobertura e do Trivy como artefactos. O workflow **Validate published** (manual) verifica o ambiente publicado ([docs/deployment.md](docs/deployment.md)).

### Testes E2E e verificação de autorização

Os testes E2E (Playwright, DA37) correm contra a aplicação com os dados de demonstração. Os cenários estão em [docs/test-plan.md](docs/test-plan.md).

```bash
# 1. Base de dados de demonstração acabada de criar e backend com o perfil demo (passos 2 e 3 acima)
docker compose down -v && docker compose up -d postgres
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# 2. Noutro terminal (o servidor do Vite arranca sozinho se não estiver a correr)
cd frontend
npx playwright install chromium   # só da primeira vez
npm run test:e2e
```

Cada teste cria dados com um nome único e apaga-os no fim, por isso pode repetir-se sem recriar a base de dados. Quando um teste falha, o relatório fica em `frontend/playwright-report/` (com trace e screenshot). Para testar o `.jar` de entrega: `MATRIZ_E2E_BASE_URL=http://localhost:8080 npm run test:e2e`.

A verificação de autorização com duas contas de demonstração (Ficha PL 04, exercício 5) corre com o backend demo ligado, a partir da raiz: `bash docs/verification/autorizacao-demo.sh`. Resultados em [docs/quality-security-review.md](docs/quality-security-review.md).

O Liquibase cria o esquema no arranque do backend (DA30). Sem o perfil `demo`, a base de dados fica vazia. Os dados de demonstração entram uma só vez: reiniciar o backend não os repete. O build para entregar gera `backend/target/matriz-1.0.0-rc.1.jar`, que serve também o frontend. Para o arrancar, a partir de `backend/` e com a base de dados ligada:

```bash
java -jar target/matriz-1.0.0-rc.1.jar --spring.profiles.active=demo
```

A aplicação fica em http://localhost:8080.

### Contas de demonstração

São as pessoas de partida de [user-stories.md](docs/user-stories.md), com dados fictícios (PRIV03). A palavra-passe de todas é `matriz-demo-2026`.

Sem o perfil `demo`, a primeira conta de Administrador é criada no arranque com os valores `MATRIZ_ADMIN_*` do `.env`, com palavra-passe temporária (DA25).

| Email | Papel | Equipas |
|-------|-------|---------|
| ana@empresa.example | Administrador | |
| rui@empresa.example | Gestor | Logística Norte |
| sofia@empresa.example | Gestor | Logística Norte |
| tiago@empresa.example | Gestor | Compras |
| marta@empresa.example | Membro | Logística Norte, Compras |
| pedro@empresa.example | Membro | Logística Norte |
| joao@empresa.example | Membro | Compras |
| carlos@empresa.example | Observador | |

## Organização da documentação

```
.
├── README.md              Visão geral (este ficheiro)
├── AGENTS.md              Instruções para agentes de IA
├── CHANGELOG.md           Alterações de cada versão
├── RELEASE_NOTES.md       Notas da versão candidata
├── openapi.yaml           Contrato da API REST
└── docs/
    ├── requirements.md    O que a Matriz tem de fazer
    ├── user-stories.md    Como se verifica que está feito
    ├── constraints.md     Limites, pressupostos e o que falta decidir
    ├── architecture.md    Como a Matriz está organizada
    ├── database.md        Que dados a Matriz guarda
    ├── implementation-plan.md  Incrementos da implementação e o seu estado
    ├── test-plan.md       Cenários de teste, resultados esperados e testes
    ├── quality-security-review.md  Code Review e Security Review
    ├── verification.md    Verificações, testes, bugs e uso da IA
    ├── deployment.md      Ambiente publicado: configuração e operação
    ├── troubleshooting.md Diagnóstico de problemas e incidentes
    ├── verification/      Evidências (prints e saídas de scripts)
    └── ui/                Navegação, ecrãs, wireframe e designs (ui/design/)
```

Código:

```
.
├── compose.yaml           Aplicação e PostgreSQL com Docker Compose (DA38)
├── Dockerfile             Imagem da aplicação (DA38)
├── .dockerignore          O que não entra na imagem
├── .env.example           Configuração com valores fictícios
├── render.yaml            Serviço no Render, o ambiente publicado (DA40)
├── .github/               Pipeline de CI, validação do ambiente publicado e Dependabot (DA39)
├── backend/               Spring Boot 4 com Maven (DA28)
└── frontend/              Vue 3 com Vite (DA28)
```

Cada tipo de informação tem **um único documento de referência**, para evitar contradições:

| Informação | Documento |
|------------|-----------|
| Visão, objetivos, definições, tipos de utilizador, permissões | [requirements.md](docs/requirements.md) |
| Requisitos funcionais (RF), regras de negócio (RN), requisitos não funcionais (RNF) | [requirements.md](docs/requirements.md) |
| Âmbito do MVP e funcionalidades futuras | [requirements.md](docs/requirements.md) |
| User Stories (US), critérios de aceitação, casos excecionais | [user-stories.md](docs/user-stories.md) |
| Restrições (R), pressupostos (P), segurança e privacidade, decisões pendentes (DP), limitações (LIM) | [constraints.md](docs/constraints.md) |
| Arquitetura, componentes e decisões técnicas (DA) | [architecture.md](docs/architecture.md) |
| Modelo de dados e decisões de modelação | [database.md](docs/database.md) |
| Contrato da API REST | [openapi.yaml](openapi.yaml) |
| Navegação, ecrãs, wireframe e decisões de interface | [ui/README.md](docs/ui/README.md) |
| Incrementos da implementação e o seu estado | [implementation-plan.md](docs/implementation-plan.md) |
| Cenários de teste (CT), resultados esperados e testes associados | [test-plan.md](docs/test-plan.md) |
| Code Review e Security Review: findings, evidências e decisões | [quality-security-review.md](docs/quality-security-review.md) |
| Resultados das verificações, bugs e uso da IA | [verification.md](docs/verification.md) |
| Ambiente publicado: configuração, entrega contínua e operação | [deployment.md](docs/deployment.md) |
| Diagnóstico de problemas e incidentes | [troubleshooting.md](docs/troubleshooting.md) |
| Alterações de cada versão | [CHANGELOG.md](CHANGELOG.md) |
| Instruções para agentes de IA | [AGENTS.md](AGENTS.md) |

Este README é só um resumo. Em caso de diferença, prevalece `docs/requirements.md`.

### Convenções de identificadores

| Prefixo | Significado | Exemplo |
|---------|-------------|---------|
| O | Objetivo | O2 |
| RF | Requisito funcional | RF17 |
| RN | Regra de negócio | RN13 |
| RNF | Requisito não funcional | RNF01 |
| US | User Story | US08 |
| E | Caso excecional | E2 |
| P | Pressuposto | P05 |
| DP | Decisão técnica pendente | DP06 |
| DA | Decisão de arquitetura, de modelação, da API ou da interface | DA01 |
| R | Restrição | R03 |
| SEG | Requisito de segurança | SEG08 |
| PRIV | Requisito de privacidade | PRIV01 |
| LIM | Limitação de âmbito | LIM01 |
| CT | Cenário de teste | CT05 |
| M | Métrica de sucesso | M1 |
| IA | Regra de desenvolvimento com IA | IA02 |

## Estado atual do projeto

| Fase | Estado |
|------|--------|
| Análise do problema e perguntas de clarificação | Concluída |
| Âmbito funcional e MVP | Concluído e conciliado com a Ficha PL 03, por validar com o Product Owner |
| User Stories e critérios de aceitação | Concluídas (22 histórias), por validar |
| Documentação de contexto | requirements.md 0.7, user-stories.md 0.7, constraints.md 1.2 |
| Instruções para agentes (AGENTS.md) | Atualizado na Ficha PL 05: fase, documentos de entrega, comandos do Docker e alterações à pipeline e ao ambiente publicado |
| Revisão crítica da documentação | Concluída: revisão 1 e revisão independente (2 agentes) aplicadas |
| Revisão crítica do Architecture Pack | Concluída: revisão 1, revisão independente e revisão final aplicadas (Ficha PL 02) |
| Arquitetura | Proposta (architecture.md 0.12), com a implantação (secção 4.1) |
| Modelo de dados | Proposta (database.md 0.3) |
| API | Contrato completo, 34 operações (openapi.yaml 0.3.2) |
| Protótipo | Wireframe da lista de projetos (Ficha PL 02, atualizado na Ficha PL 03) e descrição de todos os ecrãs (ui/README.md 0.7). Designs dos ecrãs em `docs/ui/design/` e estilo visual DA36 |
| Plano de implementação | 10 incrementos, por prioridade ([implementation-plan.md](docs/implementation-plan.md)) |
| Implementação | I0 a I6 entregues na Ficha PL 03 (tag `ficha-pl03`). I7 (resto do MVP) pronto para revisão. I8 (testes e revisão, Ficha PL 04) entregue (tag `ficha-pl04`). I9 (entrega, Ficha PL 05) na versão `1.0.0-rc.1` |
| Testes | Estratégia em três níveis (DA37) e plano de testes ([test-plan.md](docs/test-plan.md) 0.4, CT01 a CT42): 423 testes no backend (291 unitários, que cobrem todas as branches das regras de negócio), 4 de regressão no frontend e 4 percursos E2E no computador e a 360 px, todos na pipeline. Na entrega da Ficha PL 04 (tag `ficha-pl04`) eram 140, 4 e 3 |
| Qualidade e segurança | Code Review do I7 e Security Review: 17 findings, 12 correções ([quality-security-review.md](docs/quality-security-review.md) 0.3). Trivy na pipeline, sem vulnerabilidades altas ou críticas |
| Containerização | Imagem Docker e Docker Compose com a aplicação e o PostgreSQL (DA38) |
| Integração e entrega contínuas | Pipeline em quatro blocos no GitHub Actions (DA39); deploy no Render só com a pipeline aprovada (DA40) |
| Ambiente publicado | https://matriz-3lgd.onrender.com, Render e Neon em Frankfurt ([deployment.md](docs/deployment.md)) |
| Versão | `1.0.0-rc.1`, versão candidata ([CHANGELOG.md](CHANGELOG.md), [RELEASE_NOTES.md](RELEASE_NOTES.md)) |
| Verificação | Casos 01 a 09 da Ficha PL 03, resultados da Ficha PL 04 (secção 13) e da Ficha PL 05 (secção 15), testes e bugs em [verification.md](docs/verification.md) |

As decisões técnicas estão em [architecture.md, secção 9](docs/architecture.md#9-decisões-técnicas). As que faltam estão em [constraints.md, secção 4](docs/constraints.md#4-decisões-técnicas-pendentes).
