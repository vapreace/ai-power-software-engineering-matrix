# Matriz

Aplicação de gestão de projetos e tarefas para equipas, desenvolvida no curso **AI-Powered Software Engineering: From Mindset to Delivery**.

> Fase atual: **Ficha PL 03** entregue (tag `ficha-pl03`) e MVP completo com o incremento I7, pronto para revisão. Verificação em [docs/verification.md](docs/verification.md).

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

## Como executar

Requisitos: Java 21, Docker e Node 22.12 ou mais recente. Os comandos são para o Git Bash e começam na raiz do repositório.

```bash
# 1. Configuração local (só da primeira vez). Mudar MATRIZ_DB_PASSWORD no .env.
cp .env.example .env

# 2. Base de dados (PostgreSQL 18)
docker compose up -d

# 3. Backend com os dados de demonstração, em http://localhost:8080
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# 4. Frontend, noutro terminal, em http://localhost:5173
cd frontend
npm install
npm run dev
```

| Tarefa | Comando | Onde |
|--------|---------|------|
| Testes (precisam do Docker) | `./mvnw verify` | `backend/` |
| Verificar os tipos do frontend | `npm run type-check` | `frontend/` |
| Testes de regressão do frontend | `npm run test:unit` | `frontend/` |
| Build para entregar | `npm run build` e depois `./mvnw package` | `frontend/`, depois `backend/` |
| Apagar a base de dados local | `docker compose down -v` | raiz |

O Liquibase cria o esquema no arranque do backend (DA30). Sem o perfil `demo`, a base de dados fica vazia. Os dados de demonstração entram uma só vez: reiniciar o backend não os repete. O build para entregar gera `backend/target/matriz-0.0.1-SNAPSHOT.jar`, que serve também o frontend. Para o arrancar, a partir de `backend/` e com a base de dados ligada:

```bash
java -jar target/matriz-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
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
├── openapi.yaml           Contrato da API REST
└── docs/
    ├── requirements.md    O que a Matriz tem de fazer
    ├── user-stories.md    Como se verifica que está feito
    ├── constraints.md     Limites, pressupostos e o que falta decidir
    ├── architecture.md    Como a Matriz está organizada
    ├── database.md        Que dados a Matriz guarda
    ├── implementation-plan.md  Incrementos da implementação e o seu estado
    ├── test-plan.md       Cenários de teste, resultados esperados e testes
    ├── verification.md    Verificações, testes, bugs e uso da IA
    ├── verification/      Evidências (prints)
    └── ui/                Navegação, ecrãs, wireframe e designs (ui/design/)
```

Código:

```
.
├── compose.yaml           PostgreSQL local
├── .env.example           Configuração com valores fictícios
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
| Resultados das verificações, bugs e uso da IA | [verification.md](docs/verification.md) |
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
| Documentação de contexto | requirements.md 0.7, user-stories.md 0.7, constraints.md 1.1 |
| Instruções para agentes (AGENTS.md) | Atualizado na Ficha PL 02: fase, stack, documentos e convenções |
| Revisão crítica da documentação | Concluída: revisão 1 e revisão independente (2 agentes) aplicadas |
| Revisão crítica do Architecture Pack | Concluída: revisão 1, revisão independente e revisão final aplicadas (Ficha PL 02) |
| Arquitetura | Proposta (architecture.md 0.10) |
| Modelo de dados | Proposta (database.md 0.2) |
| API | Contrato completo, 34 operações (openapi.yaml 0.3.1) |
| Protótipo | Wireframe da lista de projetos (Ficha PL 02, atualizado na Ficha PL 03) e descrição de todos os ecrãs (ui/README.md 0.7). Designs dos ecrãs em `docs/ui/design/` e estilo visual DA36 |
| Plano de implementação | 8 incrementos, por prioridade ([implementation-plan.md](docs/implementation-plan.md)) |
| Implementação | I0 a I6 entregues na Ficha PL 03 (tag `ficha-pl03`). I7 (resto do MVP) pronto para revisão |
| Verificação | Casos 01 a 09 da ficha, testes e bugs em [verification.md](docs/verification.md) |

As decisões técnicas estão em [architecture.md, secção 9](docs/architecture.md#9-decisões-técnicas). As que faltam estão em [constraints.md, secção 4](docs/constraints.md#4-decisões-técnicas-pendentes).
