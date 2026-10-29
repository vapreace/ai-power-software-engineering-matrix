# Matriz

Aplicação de gestão de projetos e tarefas para equipas, desenvolvida no curso **AI-Powered Software Engineering: From Mindset to Delivery**.

> Fase atual: **conceção e arquitetura** (Ficha PL 02). Ainda não existe código.

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
| Acesso | Entrar e sair. Permissões por papel. O Administrador cria, edita, desativa e reativa contas e muda papéis. |
| Equipas | Criar e editar equipas. Adicionar e remover pessoas. |
| Projetos | Criar e editar projetos. Mudar estado e arquivar. Lista de projetos com filtro por estado. |
| Tarefas | Criar, editar e apagar. Atribuir responsável. Prazo e prioridade. Mudar estado. Lista com filtros. "As minhas tarefas". |
| Progresso | Percentagem de tarefas concluídas por projeto. Destaque e contagem de tarefas atrasadas. |

Fora do MVP: quadro, comentários, histórico, registo das ações do Administrador, pesquisa, avisos, anexos, subtarefas, relatórios, integrações. A lista completa está em [requirements.md, secção 9](docs/requirements.md#9-fora-do-âmbito-inicial).

```
 Administrador        Gestor                     Membro                Gestor
 ─────────────        ──────                     ──────                ──────
 1. Cria equipa  ──►  2. Cria projeto      ──►  4. Abre "As     ──►  6. Vê progresso
    e contas          3. Cria e atribui          minhas tarefas"      e tarefas
                         tarefas                5. Atualiza estado    atrasadas
```

## Organização da documentação

```
.
├── README.md              Visão geral (este ficheiro)
├── AGENTS.md              Instruções para agentes de IA
└── docs/
    ├── requirements.md    O que a Matriz tem de fazer
    ├── user-stories.md    Como se verifica que está feito
    ├── constraints.md     Limites, pressupostos e o que falta decidir
    └── architecture.md    Como a Matriz está organizada
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
| DA | Decisão de arquitetura | DA01 |
| R | Restrição | R03 |
| SEG | Requisito de segurança | SEG08 |
| PRIV | Requisito de privacidade | PRIV01 |
| LIM | Limitação de âmbito | LIM01 |
| M | Métrica de sucesso | M1 |
| IA | Regra de desenvolvimento com IA | IA02 |

## Estado atual do projeto

| Fase | Estado |
|------|--------|
| Análise do problema e perguntas de clarificação | Concluída |
| Âmbito funcional e MVP | Concluído, por validar com o Product Owner |
| User Stories e critérios de aceitação | Concluídas (18 histórias), por validar |
| Documentação de contexto | Concluída (versão 0.3) |
| Instruções para agentes (AGENTS.md) | Concluído, revisto no Exercício 6 |
| Revisão crítica da documentação | Concluída: revisão 1 e revisão independente (2 agentes) aplicadas |
| Arquitetura | Proposta (versão 0.1, Ficha PL 02) |
| Modelo de dados, API e protótipo | Aula 2 |
| Implementação | Ainda não iniciada |

As decisões técnicas estão em [architecture.md, secção 9](docs/architecture.md#9-decisões-técnicas). As que faltam estão em [constraints.md, secção 4](docs/constraints.md#4-decisões-técnicas-pendentes).
