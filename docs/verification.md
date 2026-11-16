# Verificação da Matriz

> **Versão:** 0.2 (Ficha PL 04: resultados dos testes na secção 13)
> **Estado:** concluído para a Ficha PL 03. Ficha PL 04 em curso.

Documentos relacionados:

- [implementation-plan.md](implementation-plan.md): incrementos e verificação prevista em cada um.
- [user-stories.md](user-stories.md): critérios de aceitação.
- [architecture.md](architecture.md): decisão de testes (DA31, DA37) e estado dos endpoints (secção 13.2).
- [test-plan.md](test-plan.md): cenários de teste (CT) e resultados esperados (Ficha PL 04).

---

## 1. Versão verificada e ambiente

| Item | Valor |
|------|-------|
| Versão | Tag `ficha-pl03` no Git. Testes no commit `ac4cc3c`; cópia limpa (M6, M7) no commit `eb9ca31` |
| Data | 15/11/2026 e 16/11/2026 (M6 e M7) |
| Sistema | Windows com Git Bash, Docker Desktop |
| Backend | Java 21, Spring Boot 4.1.2, PostgreSQL 18 em Testcontainers (testes) e Docker Compose (execução) |
| Frontend | Node 22, Vue 3.5, Vite 8 |
| Dados | Perfil `demo`, contas fictícias do README (PRIV03) |

---

## 2. Comandos executados

| Comando | Onde | Resultado |
|---------|------|-----------|
| `./mvnw verify` | `backend/` | Verde em `0a6687f` (63 testes), em `2b9bb8b` com as correções do I6 (69 testes) e em `ac4cc3c` depois da refatoração (os mesmos 69 testes, 0 falhas). |
| `npm run type-check` | `frontend/` | Sem erros. |
| `npm run test:unit` | `frontend/` | 4 testes, 0 falhas. |
| `npm run build` | `frontend/` | Build sem erros, para `backend/src/main/resources/static`. |

---

## 3. Testes automáticos

Testes de integração com MockMvc, Spring Security e PostgreSQL real em Testcontainers (DA31). O relógio está fixo em 10/11/2026 às 10:00, hora de Lisboa. Cada classe cria os seus dados antes de cada teste.

### 3.1 Os três testes exigidos pela ficha

| Exigência | Teste | O que confirma |
|-----------|-------|----------------|
| Regra de validação | `ProjectApiTests.validationRefusesAProjectNameWithOnlySpaces` | Nome só com espaços dá 400 com o erro no campo `name` (RN06). |
| Cenário de autorização | `ProjectApiTests.authorizationRefusesMembersAndProjectsOfOtherTeams` | A Marta (Membro) não cria projetos e o Pedro não abre "Fornecedores", de outra equipa (403, SEG02). |
| Criação com consulta posterior | `ProjectApiTests.createdProjectCanBeReadInALaterRequest` | O projeto criado volta a ler-se noutro pedido, com o gestor e a equipa guardados. |

### 3.2 Todos os testes

| Classe | Testes | Cobre |
|--------|--------|-------|
| `DatabaseSchemaTests` | 3 | Esquema criado pelo Liquibase; dados de demonstração só no perfil `demo`; pesquisa e ordem sem maiúsculas nem acentos (DA14, DA32). |
| `MatrizApplicationTests` | 1 | A aplicação arranca. |
| `ModularityTests` | 1 | Os módulos respeitam as dependências permitidas (DA28). |
| `SessionApiTests` | 13 | US01: entrar, credenciais erradas com a mesma mensagem, conta desativada, email sem maiúsculas, pedido sem sessão (401), palavra-passe temporária, limite de tentativas, sair, sessões separadas. |
| `SpaFallbackTests` | 4 | A raiz e os endereços da aplicação devolvem o frontend; `/api` nunca devolve o frontend; ficheiro em falta dá 404 (bug B3). |
| `ProjectApiTests` | 16 | US04, US12, US15, US17 (editar), US20, US22: criar, validar, permissões, versão antiga, projeto fechado, eliminação lógica, lista com progresso e atrasos, pesquisa, filtro vazio (bug B4), equipas visíveis. |
| `TaskApiTests` | 15 | US05, US07, US10, US11, US16: criar e ler, prioridade por omissão, título e prazo inválidos, permissões, estado, versão antiga, tarefa atrasada, apagar, ordem, filtros combinados, filtros vazios (bug B4), outras equipas. |
| `CollaborationApiTests` | 12 | US06, US07, US11, US19: vários responsáveis, sem duplicados, responsáveis inválidos, permissões, remover, responsável muda o estado, filtro por responsável, comentários com autor e hora do backend, ordem, validação, projeto fechado. |
| `DashboardApiTests` | 4 | US21: painel do Gestor e do Observador, contagens depois de criar e eliminar, Gestor sem equipa. |
| `session.spec.ts` (frontend) | 4 | Bug B1: mensagem de sessão terminada depois de recarregar; não aparece a quem não tinha entrado nem depois de Sair; só um indicador no browser. |

---

## 4. Casos da ficha

Resultado: **Passa**, **Falha** ou **Por confirmar**. A evidência indica o teste automático (A) e a verificação manual (M, secção 5).

| Caso | Cenário | Esperado | Observado | Evidência | Resultado |
|------|---------|----------|-----------|-----------|-----------|
| 01 | Acesso à aplicação | Credenciais válidas entram. Inválidas dão a mesma mensagem, sem dizer qual está errada. Pedidos sem sessão dão 401. Sair termina a sessão. | Como esperado. Depois do bug B1 corrigido, a sessão terminada mostra a mensagem também ao recarregar. | A: `SessionApiTests` (13), `session.spec.ts`. M1, M5. | Passa |
| 02 | Gestão de projetos | Criar, pesquisar, editar e eliminar de acordo com o papel e a equipa. | Como esperado. O Membro e o Observador não veem os botões e a API recusa com 403. Eliminar pede confirmação e esconde o projeto e as tarefas. | A: `ProjectApiTests`. M2 e o print [i2-projeto-criado.png](verification/i2-projeto-criado.png). | Passa |
| 03 | Persistência de tarefas | A tarefa aparece na lista sem recarregar e mantém-se depois de reiniciar o backend. | Aparece sem recarregar e mantém-se depois de recarregar a página, noutro pedido e depois de reiniciar o backend. As contagens do painel mantêm-se e os dados de demonstração não se repetem. | A: `TaskApiTests.managerCreatesATaskThatCanBeReadLater`. M3, M6. | Passa |
| 04 | Validação de dados | Obrigatórios vazios ou só com espaços e valores inválidos dão 400 com a mensagem no campo. | Como esperado no backend e no formulário. Parâmetros de filtro vazios davam 500 (bug B4), corrigido. | A: `validationRefusesAProjectNameWithOnlySpaces`, `invalidTitlesAreRefused`, `dueDateCannotBeInThePast`, `commentTextIsValidatedAndShownExactly`, `emptyStatusParameterUsesTheDefaultFilter`. M2, M3. | Passa |
| 05 | Estado e prioridade | Editar atualiza a tarefa, a lista filtrada, o progresso e as contagens do painel. | Como esperado, sem recarregar a página. | A: `assigneesManagersAndAdministratorsChangeTheStatus`, `managerEditsAndOldVersionIsRefused`, `filtersApplyTogether`, `countsChangeAfterCreatingOrDeletingTasks`. M3, M5. | Passa |
| 06 | Responsáveis | Associar e remover funciona. A mesma pessoa duas vezes dá 409 e não fica duplicada. | Como esperado. | A: `managerAssignsSeveralPeopleAndTheVersionIncreases`, `theSamePersonIsNotAssignedTwice`, `removingOneOfSeveralKeepsTheOthersAndRemovingTheLastLeavesNoAssignee`. M4. | Passa |
| 07 | Comentários | O texto, o autor autenticado e a data e hora ficam guardados e visíveis. O autor enviado no pedido é ignorado. | Como esperado. | A: `commentHasTheAuthenticatedAuthorAndTheOrganizationTime`, `commentsAreKeptInOrderAndTheObserverReadsThem`. M4. | Passa |
| 08 | Autorização na API | Mudar os identificadores no pedido não dá acesso: 403 em recursos de outras equipas e em operações sem permissão, 401 sem sessão. | Como esperado. As permissões são verificadas no backend em cada pedido, com a conta lida da base de dados (DA35). | A: `authorizationRefusesMembersAndProjectsOfOtherTeams`, `tasksOfOtherTeamsAreNotVisible`, `onlyTheManagerOrTheAdministratorCreateTasksAndOnlyInActiveProjects`, `onlyTheManagerOrTheAdministratorManageAssignees`, `requestWithoutSessionIsRefused`. M4. | Passa |
| 09 | Eliminação e erros | Eliminar respeita as dependências (as tarefas do projeto também deixam de aparecer, sem apagar dados). Um erro da API não mostra sucesso nem apaga o formulário. | Como esperado. Na versão antiga (409) o formulário mantém o que foi escrito e oferece "Abrir de novo". Erros de rede mostram "Tentar de novo". | A: `deletingKeepsTheDataButHidesTheProjectAndItsTasks`, `deletedTaskDisappearsButIsKept`, `savingOverAnOldVersionIsRefused`. M2, M3, V1. | Passa |

---

## 5. Verificações manuais

Feitas pela utilizadora, com o perfil `demo`, o backend real e `npm run dev`.

| ID | Incremento | Percurso | Resultado |
|----|------------|----------|-----------|
| M1 | I1 | Entrar com dois utilizadores de papéis diferentes, credenciais erradas, sair. | Passa |
| M2 | I2 | As seis verificações do I2: criar um projeto e vê-lo na lista, pesquisar, editar, eliminar com confirmação, papéis sem botões de gestão. Print do projeto criado. | Passa |
| M3 | I3 | Criar uma tarefa e vê-la na lista sem recarregar, recarregar a página, dados inválidos no formulário, mudar o estado, editar, apagar. | Passa |
| M4 | I4 | Associar dois responsáveis, tentar repetir, remover, comentar, filtrar por responsável; pedido direto à API com o identificador de uma tarefa de outra equipa. | Passa |
| M5 | I5 | Painel do Rui e do Carlos; criar uma tarefa e ver "Por fazer" subir sem recarregar; sessão terminada com F5. | Passa |
| M6 | I6 | Na cópia limpa do M7: criar a tarefa "Teste de reinício", parar e arrancar o backend, recarregar a página. Aparece "A sessão terminou. Entre de novo." (bug B1); depois de entrar, a tarefa continua lá, o "Por fazer" do painel é o mesmo e as tarefas de demonstração não se repetem (caso 03). | Passa (16/11/2026) |
| M7 | I6 | `git clone` do GitHub para uma pasta nova e seguir só o README: `.env`, Docker Compose, backend com o perfil `demo`, `npm install`, `npm run dev`, entrar como Rui e ver o painel com o Frota e o Novo armazém. | Passa (16/11/2026). Nenhum passo do README falhou. |

---

## 6. Verificações do frontend feitas pela IA

Percursos automáticos com Playwright (Chromium) contra uma simulação da API com os dados de demonstração. Serviram para rever a interface; os scripts não estão no repositório.

| ID | O que verificou | Resultado |
|----|-----------------|-----------|
| V1 | Todos os ecrãs para Rui, Pedro, Ana e Carlos a 360 e 1280 px: sem deslocamento horizontal e sem erros na consola. Estados a carregar, erro com "Tentar de novo", 401, 403 e 404 (ui/README.md, secção 10). | 30 verificações, todas passam |
| V2 | Percursos de criar e editar projetos e tarefas, responsáveis, comentários e filtros (I2 a I4). | Passam. Duas falhas eram do próprio script, não da aplicação (dados apagados por "Abrir de novo" e um texto mudado). |
| V3 | Cópia limpa (`git clone` do commit `ac4cc3c` para uma pasta nova): os ficheiros que o README usa estão no Git; no frontend, `npm ci`, `type-check`, `test:unit` e `build` passam. O esquema a partir do zero fica coberto pelos testes do backend (Testcontainers cria sempre uma base vazia). | Passa. Encontrou o `mvnw` sem permissão de execução no Git (falha em Linux e macOS); corrigido no commit `eb9ca31`. |

---

## 7. O que não foi possível executar

| O que | Porquê |
|-------|--------|
| Testes de carga e de tempo de resposta (RNF03, RNF04) | Fora do âmbito da Ficha PL 03. A estratégia de testes é do Módulo 4 (DP09). |
| Testes de componentes e de ponta a ponta do frontend no repositório | Idem. Só há o teste de regressão do bug B1 (DA31). |
| Verificação formal de acessibilidade (DP12) | Idem. Foi feita só a revisão da secção 7 de ui/README.md. |
| Browsers além do Chromium | Os percursos da IA usaram só o Chromium; a utilizadora usou o seu browser habitual. |
| Histórias do I7 (US02, US03, US08, US13, US14, US17 mudar gestor, US18) | Na entrega da Ficha PL 03 ainda não estavam implementadas. Foram feitas depois, no I7: ver a secção 12. |

---

## 8. Bugs encontrados

Seguem o prompt de investigação da ficha: reprodução, esperado, observado, evidência, causa (hipótese ou conclusão), correção limitada e teste de regressão.

### B1 Sessão terminada sem mensagem depois de recarregar (I1)

- **Reprodução:** entrar; deixar a sessão terminar (30 minutos sem pedidos, ou reiniciar o backend); recarregar a página ou abrir um endereço diretamente.
- **Esperado:** página de entrada com "A sessão terminou. Entre de novo." (US01, ui/README.md secção 10).
- **Observado:** página de entrada sem mensagem.
- **Evidência:** percurso V1 falhou no passo "401: entrada com a mensagem".
- **Causa (conclusão):** a mensagem só aparecia quando um pedido recebia 401 com a aplicação já aberta. Ao recarregar, a aplicação começa sem memória e não distingue "a sessão terminou" de "nunca entrou". Confirmada ao reproduzir o 401 com a página carregada e depois de recarregar.
- **Correção:** `frontend/src/session.ts` guarda no `sessionStorage` do separador só o indicador `matriz.tinhaSessao`, nunca dados da conta. A guarda de rotas acrescenta `?sessao=terminou` quando o indicador existe e a API responde 401. Sair remove o indicador.
- **Regressão:** `frontend/src/__tests__/session.spec.ts` (4 testes). Falha com o `session.ts` antigo e passa com a correção.

### B2 Linha dupla por cima da barra de progresso no painel (I5)

- **Reprodução:** abrir o Painel com um projeto que tem tarefas.
- **Esperado:** uma só linha a separar as contagens do progresso.
- **Observado:** duas linhas.
- **Evidência:** captura do percurso V1 a 1280 px.
- **Causa (conclusão):** o cartão do painel usava a classe `progress`, a mesma do componente `ProgressBar`, e as duas regras de margem e borda somavam-se.
- **Correção:** a classe do cartão passou a `progress-block`.
- **Regressão:** só visual. Verificada de novo no percurso V1; sem teste automático.

### B3 No `.jar` de entrega, recarregar um ecrã dá 404 (I0)

- **Reprodução:** `npm run build`, `./mvnw package`, arrancar o `.jar`; abrir `http://localhost:8080/projetos/1` ou recarregar qualquer ecrã que não seja o Painel.
- **Esperado:** o ecrã pedido (DA02, DA28: o frontend vai dentro do backend).
- **Observado:** erro 404. Em desenvolvimento não acontece, porque o servidor do Vite trata estes endereços.
- **Evidência:** revisão do código: não havia nenhuma regra a devolver o `index.html` para os endereços do Vue Router.
- **Causa (conclusão pela leitura do código):** o Spring Boot só serve ficheiros que existem em `static/`; `/projetos/1` não é um ficheiro.
- **Correção:** `common/SpaFallbackConfiguration` devolve o `index.html` quando o ficheiro não existe, exceto em `/api` e em ficheiros com extensão (esses continuam a dar 404).
- **Regressão:** `SpaFallbackTests` (4 testes), com um `index.html` de teste.

### B4 Filtro vazio na API dá erro 500 (I2, I3)

- **Reprodução:** `GET /api/projects?status=` ou `GET /api/projects/{id}/tasks?status=&priority=`, com sessão.
- **Esperado:** o filtro vazio é ignorado (200, com o filtro por omissão).
- **Observado:** erro 500.
- **Evidência:** revisão do código.
- **Causa (conclusão pela leitura do código):** o Spring converte o valor vazio em `null` dentro do conjunto; `ProjectQueries` e `TaskQueries` chamam `Enum::name` sobre esse `null`.
- **Correção:** `Projects.search` e `TaskService.list` removem os valores `null` antes de filtrar.
- **Regressão:** `ProjectApiTests.emptyStatusParameterUsesTheDefaultFilter` e `TaskApiTests.emptyFilterParametersAreIgnored`.

---

## 9. Revisão do código

Revisão do diff desde o início da implementação (`bd2a9e0..0a6687f`, 140 ficheiros) com apoio da IA, centrada no que a ficha pede.

| Tema | O que se verificou | Resultado |
|------|--------------------|-----------|
| Permissões | Cada operação verifica no serviço a sessão, a visibilidade (equipa) e o papel, pela ordem de DA17 (401, 404, 403, 400, 409). A conta é lida da base de dados em cada pedido (DA35). O frontend esconde botões, mas a regra está no backend. | Sem falhas. |
| Validações | Textos aparados e validados no backend (RN06, RN11), mensagens por campo; datas e prazo no passado; valores de lista. | Bug B4 nos filtros. Corrigido. |
| Tratamento de erros | Problem Details com `code` em todas as respostas de erro (DA16). O frontend trata 401, 403, 404, 409 e erros de rede sem perder o formulário. | Bug B1. Corrigido. |
| Alterações fora do âmbito | Só o que está no plano e nas histórias. A única mudança ao contrato (`manager` e `progress` no painel, openapi 0.3.1) foi aprovada e entrou primeiro no `openapi.yaml`. | Sem alterações fora do âmbito. |
| Entrega | Execução a partir do `.jar`. | Bug B3. Corrigido. |

**Ponto analisado e mantido:** ao associar ou remover um responsável, a conta inexistente dá 404 antes da verificação de permissão. Quem não gere o projeto pode assim saber se um identificador de conta existe. Mantém-se, porque segue a ordem de DA17 e os identificadores não revelam dados pessoais. Fica para rever com a estratégia de segurança (SEG02).

---

## 10. Refatoração

| Item | Registo |
|------|---------|
| Oportunidade | Os quatro testes de API (`ProjectApiTests`, `TaskApiTests`, `CollaborationApiTests`, `DashboardApiTests`) repetiam o mesmo código para preparar o MockMvc, limpar a base de dados, criar pessoas, equipas, projetos e tarefas, entrar e mudar o estado. Uma mudança no esquema ou na entrada obrigava a alterar quatro sítios. |
| Decisão | Aplicar. Novo `ApiTestSupport` (classe base comum, em `backend/src/test/java/pt/matriz`). Cada classe fica só com os seus dados de partida, os seus testes e os pedidos que só ela usa. Cada uma continua a entrar com um endereço IP próprio, por causa do limite de tentativas (DA26). |
| O que não mudou | Os testes, os dados de cada classe, as anotações do Spring (o mesmo contexto em cache) e o código da aplicação. `SessionApiTests` fica de fora: prepara as contas de outra forma, para testar a própria entrada. |
| Resultado | 345 linhas removidas e 47 acrescentadas nas quatro classes, mais as 129 da classe base. |
| Verificação | Passo separado, com commit próprio. O comportamento preserva-se se `./mvnw verify` continuar com os mesmos 69 testes e 0 falhas. |

---

## 11. Exemplos de utilização da IA

| # | Objetivo do pedido | Decisão do programador | Verificação |
|---|--------------------|------------------------|-------------|
| 1 | Rever os ecrãs desenhados contra as histórias, a ui/README.md e o contrato. | A IA encontrou o gestor e o progresso nos cartões do painel sem suporte no contrato. Decidiu-se acrescentar `manager` e `progress` a `DashboardProject` primeiro no `openapi.yaml` (0.3.1) e só depois implementar. | `DashboardApiTests` e percurso M5. |
| 2 | Implementar a criação de tarefas seguindo os slides do Módulo 3. **Sugestão rejeitada.** | O exemplo dos slides põe o Membro a criar tarefas. Rejeitado: na Matriz só o Gestor do projeto e o Administrador criam tarefas (requirements.md, secção 3.1). | `TaskApiTests.onlyTheManagerOrTheAdministratorCreateTasksAndOnlyInActiveProjects` (a Marta recebe 403). |
| 3 | Aplicar o estilo dos ecrãs desenhados ao frontend. **Sugestão corrigida.** | O desenho punha o filtro "Atrasadas" dentro do grupo Prioridade e o texto do progresso num formato próprio. Corrigido para um grupo "Atraso" separado e para o texto da US09 ("60% (6 de 10 concluídas)"). | Percursos V1 e M3. |

---

## 12. Incremento I7 (resto do MVP)

Não faz parte da entrega da Ficha PL 03 (tag `ficha-pl03`). Completa as 22 histórias do MVP.

### 12.1 Testes automáticos

| Classe | Testes | Cobre |
|--------|--------|-------|
| `AccountApiTests` | 16 | US02: criar com palavra-passe temporária, email em minúsculas e único, dados inválidos, editar o email e entrar com ele, só o Administrador, filtros da lista, repor o acesso (limpa a pausa de tentativas). US14: desativar (tarefas abertas "Sem responsável", concluídas mantêm o nome, também em projeto arquivado), sessão aberta recusada, projetos "Sem gestor", reativar sem devolver tarefas, último Administrador. US18: Gestor passa a Membro, Membro passa a Observador (sai das equipas e das tarefas abertas), efeito no pedido seguinte. |
| `TeamApiTests` | 10 | US03: criar, nomes inválidos ou repetidos (sem distinguir maiúsculas), mudar o nome, pessoa em duas equipas, só Gestor e Membro com conta ativa, remover (tarefas abertas da equipa e projetos "Sem gestor"), quem vê as pessoas, efeito no pedido seguinte, só o Administrador. |
| `ProjectStatusApiTests` | 9 | US13: concluir, reabrir, arquivar e desarquivar, transições recusadas, projeto arquivado só de leitura, permissões, versão antiga. US17: o Administrador muda o gestor (também em projeto arquivado), o Gestor não pode, gestores inválidos, novo gestor de um projeto "Sem gestor". |
| `MyTasksApiTests` | 6 | US08: tarefas abertas de projetos ativos com o projeto, "Incluir concluídas", projeto concluído fica de fora, ordem de RN24, títulos sem maiúsculas nem acentos, responsáveis removidos e tarefas apagadas de fora, lista vazia. |

`./mvnw verify` verde no commit `42b637d` (16/11/2026): 110 testes (69 da Ficha PL 03 e 41 do I7), 0 falhas.

### 12.2 Verificações do frontend feitas pela IA

| ID | O que verificou | Resultado |
|----|-----------------|-----------|
| V4 | Percursos com Playwright contra a simulação da API: As minhas tarefas (Marta, com e sem concluídas; Rui sem tarefas), concluir com confirmação de RN10 e cancelar, reabrir, mudar o gestor de um projeto arquivado, criar equipa (nome repetido recusado), adicionar e remover pessoa com confirmação, projeto "Sem gestor" depois de remover o gestor, criar conta com validações e email repetido, aviso ao mudar o papel, desativar com confirmação, último Administrador, reativar, repor o acesso, Observador sem ações e sem acesso a Contas. Ecrãs novos a 360 e 1280 px para Ana, Marta e Carlos, sem deslocamento horizontal e sem erros na consola. | 43 verificações, todas passam. Os percursos do I2 ao I5 continuam a passar. |

### 12.3 Verificações manuais

| ID | Percurso | Resultado |
|----|----------|-----------|
| M8 | Marta: "As minhas tarefas" mostra as tarefas abertas dos projetos ativos, por prazo; "Incluir concluídas" acrescenta as concluídas. | Passa (16/11/2026) |
| M9 | Rui: concluir o Novo armazém (aparece a confirmação com o número de tarefas abertas), reabrir; Ana: mudar o gestor do Armazém Sul para a Sofia. | Passa (16/11/2026) |
| M10 | Ana: criar uma equipa, adicionar e remover uma pessoa; remover o Rui da Logística Norte deixa o Novo armazém "Sem gestor". | Passa (16/11/2026) |
| M11 | Ana: criar uma conta e entrar com ela (pede palavra-passe nova); desativar o Pedro (não entra e as tarefas abertas ficam "Sem responsável"); reativar; tentar desativar a própria conta (recusado). | Passa (16/11/2026) |

---

## 13. Ficha PL 04

Resultados das execuções dos testes da Ficha PL 04. Os cenários e os resultados esperados estão em [test-plan.md](test-plan.md); a estratégia em DA37.

### 13.1 Testes unitários (exercício 2)

| Item | Registo |
|------|---------|
| Teste | `TaskTitleRulesTests` (`backend/src/test/java/pt/matriz/tasks/application`) |
| Regra | RN11: título obrigatório, 1 a 200 caracteres depois de retirar os espaços nas pontas e de normalizar em NFC (DA11), cada código Unicode conta um (LIM13). |
| Cenários | CT04 a CT08: 7 títulos válidos (incluindo os valores-limite de 1 e 200 caracteres, acentos combinados, emojis e espaços nas pontas), 7 inválidos (em falta, vazio, só espaços, 201 caracteres, 202 códigos Unicode) e 1 edição com título só com espaços. |
| Como | `TaskService` real; repositório, consultas e outros módulos com mocks do Mockito; Relógio da organização fixo em 10/11/2026. Sem base de dados nem Spring. Padrão Arrange, Act, Assert. |
| Revisão das asserções | Cada caso verifica o valor da RN11 e não o que o código faz: o título gravado, o estado "Por fazer" e a prioridade Média nos válidos; 400 `VALIDATION_ERROR` com o erro no campo `title` e nenhuma gravação nos inválidos. A mensagem é a mesma do teste de integração `TaskApiTests.invalidTitlesAreRefused`. Nenhum teste precisou de correção e a regra não mudou. |
| Comando | `./mvnw test -Dtest=TaskTitleRulesTests` em `backend/` (não precisa do Docker) |
| Resultado | 15 testes, 0 falhas, 0 erros, 3,5 s (16/11/2026, Windows com Git Bash; relatório em `backend/target/surefire-reports`). |

### 13.2 Testes de integração (exercício 3)

| Item | Registo |
|------|---------|
| Testes | `TaskApiTests.createdTaskIsStoredAndReturnedByTheApi` e `TaskApiTests.invalidTitleStoresNothingAndUnknownTaskIsNotFound` |
| Cenários | CT01 e CT02: o Rui cria uma tarefa pela API; 201 com `Location` e os campos do schema `Task` de openapi.yaml; a linha fica gravada em `tasks` com os mesmos valores, sem responsáveis e não eliminada; o Pedro lê-a noutro pedido pelo endereço do `Location`, e a tarefa aparece na lista do projeto. CT05 e CT22 (negativos): título só com espaços dá 400 `VALIDATION_ERROR` no campo `title` sem gravar nada; uma tarefa que nunca existiu dá 404 `NOT_FOUND`. |
| Ambiente | API real com MockMvc e Spring Security; PostgreSQL 18 em Testcontainers, com uma base nova em cada execução; tabelas limpas antes de cada teste (`ApiTestSupport`); Relógio da organização fixo em 10/11/2026. |
| Comando | `./mvnw test -Dtest=TaskApiTests` em `backend/` (com o Docker) |
| Resultado | 17 testes (15 do I3 e os 2 novos), 0 falhas, 0 erros, 40 s (16/11/2026, Windows com Git Bash e Docker Desktop). |

### 13.3 Testes E2E (exercício 4)

| Item | Registo |
|------|---------|
| Testes | `frontend/e2e/create-task.spec.ts` (CT03), `frontend/e2e/project-flow.spec.ts` (CT33) e `frontend/e2e/direct-access.spec.ts` (CT20) |
| Percursos | CT03, fluxo da ficha: o Rui entra, abre o Novo armazém, cria uma tarefa e vê-a na lista "Por fazer" e "Sem responsável", sem recarregar. CT33, fluxo do slide do Módulo 4 e do prompt E2E dos anexos: o Rui cria um projeto e uma tarefa, muda o estado para "Em curso" e o painel mostra 0, 1 e 0. CT20: o Carlos (Observador) dá o endereço de Fornecedores; o Pedro (Membro da Logística Norte) não o vê na lista e, ao abri-lo diretamente, vê "Não tem acesso a esta página." sem dados do projeto. |
| Como | Playwright 1.65.1 com Chromium (perfil "Desktop Chrome"); seletores por papel e rótulo (`getByRole`, `getByLabel`), sem `data-testid` e sem esperas fixas; o identificador do que é criado vem da resposta 201 da API. Cada teste usa nomes únicos e apaga pela API o que criou (eliminação lógica, RN32). Trace e screenshot só em caso de falha. |
| Ambiente | Base de dados de demonstração recriada antes da execução (`docker compose down -v` e `up -d`), porque as verificações manuais M9 a M11 tinham mudado os dados de partida. Backend com o perfil `demo` e servidor do Vite. |
| Comando | `npm run test:e2e` em `frontend/`, com o backend a correr (o Vite arranca sozinho se não estiver a correr). Uma vez antes: `npx playwright install chromium`. `npm run type-check` sem erros, já com os testes E2E. |
| Resultado | 3 testes, 3 passam, 0 falhas e 0 repetições, 14 s (16/11/2026, Windows). Executado duas vezes seguidas com o mesmo resultado, sem limpar a base de dados entre execuções: os testes não dependem de execuções anteriores. Relatório em `frontend/playwright-report/` e `frontend/test-results/results.json` (fora do Git). |

### 13.4 Code Review e Security Review (exercício 5)

Findings, evidências e decisões em [quality-security-review.md](quality-security-review.md). Aqui ficam só as execuções.

| Verificação | Comando | Resultado |
|-------------|---------|-----------|
| `AccessControlApiTests` (CT13 a CT19, CT21, CT31, CT32; CT34 depois das correções) | `./mvnw test -Dtest=AccessControlApiTests` em `backend/` | Primeira execução: 2 falhas, problema do teste (asserção sobre `$.title`, que também é campo do Problem Details). Depois da correção do teste: 10 testes, 0 falhas, 30 s (16/11/2026). |
| Autorização com duas contas de demonstração | `bash docs/verification/autorizacao-demo.sh \| tee docs/verification/autorizacao-demo.txt`, com a base de dados de demonstração recriada e o backend com o perfil `demo` | Pedro: 403 em 10 pedidos de leitura e alteração sobre Fornecedores; 400 no comentário (acento mal codificado no Git Bash, corrigido no script). Ana: tarefa, comentários e equipa iguais no fim. Revelou F1 e F2 (16/11/2026, 13:26). |
| ESLint | `npm run lint` em `frontend/` | 60 ficheiros, 0 erros, 0 avisos (numa cópia em Linux e no Windows, 16/11/2026). |
| Semgrep 1.185 | Regras abertas do repositório `semgrep-rules` (Java, TypeScript, JavaScript de browser, YAML, segredos), numa cópia do repositório | 0 findings de código; 1 falso positivo de segredos (F12). |
| `npm audit` | `npm audit` e `npm audit --omit=dev` em `frontend/` | Produção: 0. Desenvolvimento: 7 moderadas no Windows, 9 em Linux, todas da cadeia `minimatch` (F7). |
| `type-check` e testes de regressão do frontend | `npm run type-check`, `npm run test:unit` | Sem erros; 4 testes, 0 falhas. |
| Depois das correções F1, F2, F4, F8, F9 e F10 | `./mvnw verify` em `backend/` | 140 testes (110 de antes, 15 unitários, 2 do exercício 3, 11 de `AccessControlApiTests` e 2 novos em `TeamApiTests`), 0 falhas, `BUILD SUCCESS`, 2 min 17 s (16/11/2026). |
| | `npm run type-check && npm run lint && npm run test:unit && npm run build` em `frontend/` | Sem erros; 4 testes, 0 falhas; build sem erros. |
| | Script de autorização repetido com o backend reiniciado | 403 em todos os pedidos do Pedro, incluindo o comentário e os quatro pedidos de F1 e F2 (antes: 404 em dois deles). A Ana confirma que nada mudou (16/11/2026, 14:18). |
