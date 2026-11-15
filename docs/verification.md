# Verificação da Matriz

> **Versão:** 0.1 (Ficha PL 03, exercícios 8 e 9, incremento I6)
> **Estado:** em curso. Os resultados marcados "Por confirmar" dependem de uma verificação manual ainda não feita.

Documentos relacionados:

- [implementation-plan.md](implementation-plan.md): incrementos e verificação prevista em cada um.
- [user-stories.md](user-stories.md): critérios de aceitação.
- [architecture.md](architecture.md): decisão de testes (DA31) e estado dos endpoints (secção 13.2).

---

## 1. Versão verificada e ambiente

| Item | Valor |
|------|-------|
| Versão | Tag `ficha-pl03` no Git (última verificação completa antes do I6: commit `0a6687f`) |
| Data | 15/11/2026 |
| Sistema | Windows com Git Bash, Docker Desktop |
| Backend | Java 21, Spring Boot 4.1.2, PostgreSQL 18 em Testcontainers (testes) e Docker Compose (execução) |
| Frontend | Node 22, Vue 3.5, Vite 8 |
| Dados | Perfil `demo`, contas fictícias do README (PRIV03) |

---

## 2. Comandos executados

| Comando | Onde | Resultado |
|---------|------|-----------|
| `./mvnw verify` | `backend/` | Verde em `0a6687f` (63 testes) e em `2b9bb8b`, com as correções do I6 (69 testes, 0 falhas). Depois da refatoração: os mesmos 69 testes, a confirmar. |
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
| 03 | Persistência de tarefas | A tarefa aparece na lista sem recarregar e mantém-se depois de reiniciar o backend. | Aparece sem recarregar e mantém-se depois de recarregar a página e noutro pedido. Falta reiniciar o backend. | A: `TaskApiTests.managerCreatesATaskThatCanBeReadLater`. M3. M6 por fazer. | Por confirmar |
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
| M6 | I6 | Criar uma tarefa, parar e arrancar o backend, confirmar que a tarefa continua lá (caso 03). | Por fazer |
| M7 | I6 | `git clone` para uma pasta nova e seguir só o README até entrar na aplicação. | Por fazer |

---

## 6. Verificações do frontend feitas pela IA

Percursos automáticos com Playwright (Chromium) contra uma simulação da API com os dados de demonstração. Serviram para rever a interface; os scripts não estão no repositório.

| ID | O que verificou | Resultado |
|----|-----------------|-----------|
| V1 | Todos os ecrãs para Rui, Pedro, Ana e Carlos a 360 e 1280 px: sem deslocamento horizontal e sem erros na consola. Estados a carregar, erro com "Tentar de novo", 401, 403 e 404 (ui/README.md, secção 10). | 30 verificações, todas passam |
| V2 | Percursos de criar e editar projetos e tarefas, responsáveis, comentários e filtros (I2 a I4). | Passam. Duas falhas eram do próprio script, não da aplicação (dados apagados por "Abrir de novo" e um texto mudado). |

---

## 7. O que não foi possível executar

| O que | Porquê |
|-------|--------|
| Testes de carga e de tempo de resposta (RNF03, RNF04) | Fora do âmbito da Ficha PL 03. A estratégia de testes é do Módulo 4 (DP09). |
| Testes de componentes e de ponta a ponta do frontend no repositório | Idem. Só há o teste de regressão do bug B1 (DA31). |
| Verificação formal de acessibilidade (DP12) | Idem. Foi feita só a revisão da secção 7 de ui/README.md. |
| Browsers além do Chromium | Os percursos da IA usaram só o Chromium; a utilizadora usou o seu browser habitual. |
| Histórias do I7 (US02, US03, US08, US13, US14, US17 mudar gestor, US18) | Ainda não implementadas. As 12 operações respetivas estão no contrato com o estado "Contrato (I7)" (architecture.md, secção 13.2). |

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
