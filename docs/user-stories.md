# User Stories do MVP

> **Versão:** 0.2 (revisão crítica 1, Ficha PL 01)
> **Estado:** em validação com o Product Owner.

## Como ler este documento

- Os termos "tarefa aberta", "tarefa atrasada", "progresso", "data atual" e "projetos visíveis" têm o significado definido em [requirements.md, secção 2](requirements.md#2-definições).
- Cada história indica os requisitos (RF) e as regras de negócio (RN) que cobre. Em caso de conflito, prevalece [requirements.md](requirements.md).
- Os critérios de aceitação usam o formato Gherkin (Given / When / Then). As palavras-chave ficam em inglês e o conteúdo em português.
- Os nomes usados nos exemplos e os emails são fictícios. Os emails usam o domínio reservado `empresa.example`.
- **"Recusado"** significa sempre: a operação não altera nada e aparece uma mensagem de erro. Quando o texto da mensagem importa, o cenário indica-o.
- Cada cenário pode alterar, no seu `Given`, a situação de partida descrita na tabela seguinte.

### Pessoas usadas nos exemplos

| Nome | Papel | Equipas |
|------|-------|---------|
| Ana | Administrador | |
| Rui | Gestor | Logística Norte |
| Sofia | Gestor | Logística Norte |
| Tiago | Gestor | Compras |
| Marta | Membro | Logística Norte, Compras |
| Pedro | Membro | Logística Norte |
| João | Membro | Compras |
| Carlos | Observador | |

---

## 1. Backlog priorizado

Todas as histórias são **Must** e fazem parte do MVP. A ordem segue as dependências: primeiro o que permite testar o resto, depois o fluxo que entrega valor.

| Ordem | ID | História | Papel | Requisitos | Depende de |
|:-----:|----|----------|-------|------------|------------|
| 1 | US01 | Entrar na aplicação | Todos | RF01 | |
| 2 | US02 | Criar conta | Administrador | RF02, RF03 | US01 |
| 3 | US03 | Criar equipa e gerir quem lhe pertence | Administrador | RF05, RF06 | US02 |
| 4 | US04 | Criar projeto | Gestor | RF07 | US03 |
| 5 | US05 | Criar tarefa | Gestor | RF12, RF14 | US04 |
| 6 | US06 | Atribuir responsável | Gestor | RF13 | US05 |
| 7 | US07 | Mudar estado da tarefa | Membro, Gestor | RF15 | US06 |
| 8 | US08 | Ver "As minhas tarefas" | Membro | RF17 | US06 |
| 9 | US09 | Ver progresso do projeto | Gestor | RF27 | US07 |
| 10 | US10 | Identificar tarefas atrasadas | Gestor | RF28 | US05 |
| 11 | US11 | Filtrar tarefas do projeto | Gestor | RF16 | US05 |
| 12 | US12 | Ver projetos das minhas equipas | Membro | RF09, RF03 | US04 |
| 13 | US13 | Mudar estado e arquivar projeto | Gestor | RF08 | US04 |
| 14 | US17 | Editar projeto | Gestor, Administrador | RF07 | US04 |
| 15 | US14 | Desativar conta | Administrador | RF02 | US06 |
| 16 | US18 | Mudar papel de uma conta | Administrador | RF02 | US17 |
| 17 | US15 | Consultar todos os projetos | Observador | RF03, RF09 | US09 |
| 18 | US16 | Editar e apagar tarefa | Gestor | RF12 | US05 |

---

## 2. Histórias e critérios de aceitação

### US01 Entrar na aplicação

> **Como** utilizador, **quero** entrar na Matriz com o meu email e palavra-passe, **para** aceder apenas à informação que o meu papel permite.

**Requisitos:** RF01 · **Segurança:** SEG04, SEG05, SEG09

```gherkin
Scenario: credenciais válidas
  Given existe uma conta ativa com o email "marta@empresa.example"
  When a Marta entra com o email e a palavra-passe corretos
  Then fica autenticada e o seu nome fica visível na página

Scenario: credenciais inválidas
  Given existe uma conta ativa com o email "marta@empresa.example"
  When a Marta entra com uma palavra-passe errada
  Then a entrada é recusada
  And aparece a mensagem "Email ou palavra-passe incorretos"

Scenario: conta desativada
  Given a conta "pedro@empresa.example" está desativada
  When o Pedro entra com o email e a palavra-passe corretos
  Then a entrada é recusada
  And aparece a mensagem "Conta desativada. Contacte o administrador."

Scenario: demasiadas tentativas falhadas
  Given houve 5 tentativas falhadas seguidas para a conta "marta@empresa.example" nos últimos 15 minutos
  When a Marta entra com o email e a palavra-passe corretos
  Then a entrada é recusada
  And aparece a mensagem "Demasiadas tentativas. Tente novamente mais tarde."

Scenario: fim do bloqueio
  Given a conta "marta@empresa.example" foi bloqueada por tentativas falhadas há mais de 15 minutos
  When a Marta entra com o email e a palavra-passe corretos
  Then fica autenticada
```

A mensagem de credenciais inválidas é a mesma quer falhe o email, quer a palavra-passe.

---

### US02 Criar conta

> **Como** Administrador, **quero** criar uma conta com nome, email e papel, **para** que um novo colaborador possa usar a Matriz.

**Requisitos:** RF02, RF03 · **Regras:** RN04

```gherkin
Scenario: criação com sucesso
  Given a Ana tem papel Administrador
  When cria uma conta com nome "Marta Silva", email "marta@empresa.example" e papel "Membro"
  Then a conta aparece na lista de utilizadores com estado "Ativa"

Scenario: email repetido
  Given já existe uma conta com o email "marta@empresa.example"
  When a Ana cria outra conta com o mesmo email
  Then a criação é recusada
  And aparece a mensagem "Já existe uma conta com este email"

Scenario: campo obrigatório em falta
  When a Ana cria uma conta sem nome, sem email ou sem papel
  Then a criação é recusada

Scenario: utilizador sem permissão
  Given o Rui tem papel Gestor
  When tenta abrir a gestão de contas
  Then o acesso é recusado
```

O papel é um de: Administrador, Gestor, Membro, Observador.

A forma como o novo utilizador recebe o acesso inicial está pendente ([constraints.md, DP07](constraints.md#4-decisões-técnicas-pendentes)).

---

### US03 Criar equipa e gerir quem lhe pertence

> **Como** Administrador, **quero** criar equipas e adicionar ou remover pessoas, **para** definir quem vê cada projeto.

**Requisitos:** RF05, RF06 · **Regras:** RN02, RN03, RN17, RN22

```gherkin
Scenario: criar equipa
  When a Ana cria a equipa "Logística Norte"
  Then a equipa aparece na lista de equipas

Scenario: nome repetido
  Given já existe a equipa "Compras"
  When a Ana cria outra equipa com o nome "Compras"
  Then a criação é recusada

Scenario: pessoa em duas equipas
  Given existem as equipas "Logística Norte" e "Compras"
  When a Ana adiciona a Marta às duas equipas
  Then a Marta aparece nas duas equipas

Scenario: remover pessoa com tarefas abertas
  Given a Marta é responsável por 2 tarefas abertas num projeto de "Compras"
  When a Ana remove a Marta da equipa "Compras"
  Then essas 2 tarefas ficam com responsável "Sem responsável"

Scenario: remover o gestor de um projeto
  Given o Rui é gestor do projeto "Novo armazém" da equipa "Logística Norte"
  When a Ana remove o Rui da equipa "Logística Norte"
  Then o projeto "Novo armazém" fica "Sem gestor"

Scenario: contas desativadas não podem ser adicionadas
  Given a conta do Pedro está desativada
  When a Ana procura pessoas para adicionar a uma equipa
  Then o Pedro não aparece na lista
```

---

### US04 Criar projeto

> **Como** Gestor, **quero** criar um projeto numa equipa a que pertenço, **para** agrupar as tarefas desse trabalho.

**Requisitos:** RF07 · **Regras:** RN01, RN05, RN06, RN07

```gherkin
Scenario: criação com sucesso
  Given o Rui tem papel Gestor e pertence à equipa "Logística Norte"
  When cria o projeto "Novo armazém" na equipa "Logística Norte"
    com data de início 2026-11-02 e data de fim prevista 2026-12-18
  Then o projeto é criado com estado "Ativo"
  And o gestor do projeto é o Rui
  And o projeto aparece na lista de projetos da equipa

Scenario: datas incoerentes
  When o Rui cria um projeto com data de início 2026-12-18
    e data de fim prevista 2026-11-02
  Then a criação é recusada
  And aparece a mensagem "A data de fim prevista não pode ser anterior à data de início"

Scenario: nome em falta
  When o Rui cria um projeto sem nome
  Then a criação é recusada

Scenario: equipa a que não pertence
  Given o Rui não pertence à equipa "Compras"
  When tenta criar um projeto na equipa "Compras"
  Then a criação é recusada

Scenario: Membro tenta criar projeto
  Given a Marta tem papel Membro
  When tenta criar um projeto
  Then a criação é recusada
```

---

### US05 Criar tarefa

> **Como** Gestor, **quero** criar tarefas num projeto que giro, com título, descrição, prazo e prioridade, **para** dividir o trabalho em partes que se podem acompanhar.

**Requisitos:** RF12, RF14 · **Regras:** RN08, RN11, RN12, RN13, RN20

```gherkin
Scenario: criação com sucesso
  Given o projeto "Novo armazém" tem estado "Ativo" e o gestor é o Rui
  And a data atual é 2026-11-05
  When o Rui cria a tarefa "Pedir orçamento de estantes"
    com prazo 2026-11-15 e prioridade "Alta"
  Then a tarefa é criada com estado "Por fazer" e responsável "Sem responsável"
  And o total de tarefas do projeto aumenta em 1

Scenario: prioridade não indicada
  When o Rui cria uma tarefa sem indicar prioridade
  Then a tarefa fica com prioridade "Média"

Scenario: prazo no passado
  Given a data atual é 2026-11-05
  When o Rui cria uma tarefa com prazo 2026-11-04
  Then a criação é recusada
  And aparece a mensagem "O prazo não pode ser anterior à data atual"

Scenario Outline: título inválido
  When o Rui cria uma tarefa com <título>
  Then a criação é recusada

  Examples:
    | título                     |
    | título vazio               |
    | título com 201 caracteres  |

Scenario: título no limite
  When o Rui cria uma tarefa com um título de 200 caracteres
  Then a tarefa é criada

Scenario Outline: projeto que não aceita tarefas
  Given o projeto "Armazém Sul" tem estado "<estado>"
  When o Rui tenta criar uma tarefa nesse projeto
  Then a criação é recusada

  Examples:
    | estado    |
    | Concluído |
    | Arquivado |
```

---

### US06 Atribuir responsável

> **Como** Gestor, **quero** atribuir ou mudar o responsável de uma tarefa, **para** que fique claro quem a executa.

**Requisitos:** RF13 · **Regras:** RN14

```gherkin
Scenario: atribuir
  Given a tarefa "Pedir orçamento de estantes" está "Sem responsável"
  And a Marta tem conta ativa e pertence à equipa do projeto
  When o Rui atribui a tarefa à Marta
  Then o responsável passa a ser a Marta
  And a tarefa aparece em "As minhas tarefas" da Marta

Scenario: pessoa fora da equipa
  Given o João não pertence à equipa do projeto
  When o Rui tenta atribuir a tarefa ao João
  Then a atribuição é recusada

Scenario: papel que não pode ser responsável
  Given o Carlos tem papel Observador e pertence à equipa do projeto
  When o Rui tenta atribuir a tarefa ao Carlos
  Then a atribuição é recusada

Scenario: retirar responsável
  Given a Marta é responsável pela tarefa
  When o Rui retira o responsável
  Then a tarefa fica "Sem responsável"
  And deixa de aparecer em "As minhas tarefas" da Marta
```

---

### US07 Mudar estado da tarefa

> **Como** Membro, **quero** mudar o estado das tarefas de que sou responsável, **para** que o gestor saiba em que ponto está o trabalho.

**Requisitos:** RF15 · **Regras:** RN09, RN12, RN15

```gherkin
Scenario: avançar o estado
  Given a Marta é responsável pela tarefa "Pedir orçamento de estantes" em "Por fazer"
  When muda o estado para "Em curso"
  Then o estado da tarefa é "Em curso"

Scenario: reabrir tarefa concluída
  Given o projeto mostra "70% (7 de 10 concluídas)"
  And a Marta é responsável por uma das tarefas concluídas
  When a Marta muda o estado dessa tarefa para "Em curso"
  Then o estado é "Em curso"
  And o projeto passa a mostrar "60% (6 de 10 concluídas)"

Scenario: tarefa de outra pessoa
  Given o Pedro é o responsável pela tarefa "Contratar transporte"
  When a Marta tenta mudar o estado dessa tarefa
  Then a alteração é recusada
  And o estado mantém-se

Scenario: gestor muda qualquer tarefa do seu projeto
  Given o Rui é gestor do projeto "Novo armazém"
  When muda o estado de uma tarefa da Marta para "Concluída"
  Then o estado é "Concluída"

Scenario: gestor responsável num projeto que não gere
  Given a Sofia tem papel Gestor e é gestora do projeto "Frota"
  And o Rui é responsável por uma tarefa do projeto "Frota"
  When o Rui muda o estado dessa tarefa para "Em curso"
  Then o estado é "Em curso"

Scenario Outline: projeto que não aceita alterações às tarefas
  Given o projeto tem estado "<estado>"
  When a Marta tenta mudar o estado de uma tarefa de que é responsável nesse projeto
  Then a alteração é recusada

  Examples:
    | estado    |
    | Concluído |
    | Arquivado |
```

Entre os três estados da tarefa, qualquer mudança é permitida (RN12).

---

### US08 Ver "As minhas tarefas"

> **Como** Membro, **quero** ver numa só página todas as tarefas de que sou responsável, de todos os projetos, **para** saber o que tenho para fazer.

**Requisitos:** RF17 · **Regras:** RN24

```gherkin
Scenario: tarefas de várias equipas
  Given a Marta pertence às equipas "Logística Norte" e "Compras"
  And é responsável por 3 tarefas abertas em "Logística Norte",
    2 tarefas abertas em "Compras" e 1 tarefa concluída
  When abre "As minhas tarefas"
  Then vê 5 tarefas
  And cada tarefa mostra projeto, prazo, prioridade e estado

Scenario: incluir concluídas
  When a Marta ativa o filtro "Incluir concluídas"
  Then vê 6 tarefas

Scenario: ordenação
  Given a Marta tem as tarefas abertas:
    | título    | prazo      | prioridade |
    | Inventário | 2026-11-20 | Baixa      |
    | Orçamento | 2026-11-12 | Média      |
    | Transporte | 2026-11-12 | Alta       |
    | Arquivo   | sem prazo  | Alta       |
    | Contrato  | 2026-11-12 | Alta       |
  When abre "As minhas tarefas"
  Then as tarefas aparecem pela ordem: Contrato, Transporte, Orçamento, Inventário, Arquivo

Scenario: projetos arquivados
  Given a Marta tem uma tarefa aberta num projeto "Arquivado"
  When abre "As minhas tarefas"
  Then essa tarefa não aparece

Scenario: sem tarefas
  Given a Marta não é responsável por nenhuma tarefa aberta
  When abre "As minhas tarefas"
  Then vê a mensagem "Não tem tarefas atribuídas"
```

---

### US09 Ver progresso do projeto

> **Como** Gestor, **quero** ver o progresso de cada projeto, **para** saber se o trabalho avança como previsto.

**Requisitos:** RF27

```gherkin
Scenario: cálculo base
  Given o projeto "Novo armazém" tem 10 tarefas, 6 delas concluídas
  When o Rui abre o projeto
  Then vê "60% (6 de 10 concluídas)"
  And o mesmo valor aparece na lista de projetos

Scenario: arredondamento para baixo
  Given o projeto tem 3 tarefas, 2 delas concluídas
  When o Rui abre o projeto
  Then vê "66% (2 de 3 concluídas)"

Scenario: projeto sem tarefas
  Given o projeto não tem tarefas
  When o Rui abre o projeto
  Then vê "Sem tarefas" em vez de uma percentagem

Scenario: valor atualizado
  Given o projeto mostra "60% (6 de 10 concluídas)"
  When uma tarefa aberta passa a "Concluída"
  And o Rui abre de novo o projeto
  Then vê "70% (7 de 10 concluídas)"
```

---

### US10 Identificar tarefas atrasadas

> **Como** Gestor, **quero** ver que tarefas estão atrasadas, **para** agir antes que o atraso aumente.

**Requisitos:** RF28

```gherkin
Scenario Outline: classificação de atraso
  Given a data atual é 2026-11-10
  And uma tarefa tem prazo "<prazo>" e estado "<estado>"
  When o Rui abre o projeto
  Then a tarefa <resultado> marcada como "Atrasada"

  Examples:
    | prazo      | estado    | resultado |
    | 2026-11-09 | Em curso  | está      |
    | 2026-11-09 | Por fazer | está      |
    | 2026-11-10 | Em curso  | não está  |
    | 2026-11-09 | Concluída | não está  |
    | sem prazo  | Por fazer | não está  |

Scenario Outline: contagem no projeto
  Given o projeto tem exatamente <n> tarefas atrasadas
  When o Rui abre o projeto ou a lista de projetos
  Then vê "<texto>"

  Examples:
    | n | texto               |
    | 0 | Sem atrasos         |
    | 1 | 1 tarefa atrasada   |
    | 2 | 2 tarefas atrasadas |
```

---

### US11 Filtrar tarefas do projeto

> **Como** Gestor, **quero** filtrar as tarefas de um projeto por responsável, estado e atraso, **para** encontrar as tarefas que preciso de acompanhar.

**Requisitos:** RF16

```gherkin
Scenario: filtro por estado
  Given o projeto tem 3 tarefas "Por fazer", 2 "Em curso" e 5 "Concluída"
  When o Rui filtra por estado "Em curso"
  Then vê apenas 2 tarefas

Scenario: filtros combinados
  Given o projeto tem 4 tarefas da Marta, das quais 1 está atrasada
  When o Rui filtra por responsável "Marta" e por "Atrasadas"
  Then vê apenas 1 tarefa

Scenario: tarefas sem responsável
  When o Rui filtra por "Sem responsável"
  Then vê apenas as tarefas sem responsável

Scenario: sem resultados
  When o Rui aplica filtros sem nenhuma tarefa correspondente
  Then vê a mensagem "Nenhuma tarefa corresponde aos filtros"
```

Vários filtros aplicam-se ao mesmo tempo: a tarefa tem de cumprir todos.

---

### US12 Ver projetos das minhas equipas

> **Como** Membro, **quero** ver a lista de projetos das minhas equipas, filtrada por estado, **para** encontrar os projetos onde trabalho.

**Requisitos:** RF09, RF03 · **Segurança:** SEG02

```gherkin
Scenario: só projetos das minhas equipas
  Given o Pedro pertence apenas à equipa "Logística Norte"
  When abre a lista de projetos
  Then vê apenas projetos da equipa "Logística Norte"
  And cada linha mostra nome, equipa, gestor, estado, progresso e nº de tarefas atrasadas

Scenario: arquivados escondidos por omissão
  When o Pedro abre a lista de projetos
  Then os projetos "Arquivado" não aparecem
  And aparecem quando ativa o filtro de estado "Arquivado"

Scenario: acesso direto a projeto de outra equipa
  Given o Pedro não pertence à equipa "Compras"
  When tenta abrir diretamente o endereço de um projeto da equipa "Compras"
  Then o acesso é recusado
```

---

### US13 Mudar estado e arquivar projeto

> **Como** Gestor, **quero** mudar o estado de um projeto que giro e arquivá-lo, **para** separar o trabalho ativo do terminado.

**Requisitos:** RF08 · **Regras:** RN07, RN09, RN10

```gherkin
Scenario: concluir com tarefas abertas
  Given o projeto "Novo armazém" está "Ativo" e tem 3 tarefas abertas
  When o Rui muda o estado para "Concluído"
  Then aparece a mensagem "Este projeto tem 3 tarefas abertas. Concluir mesmo assim?"

Scenario: confirmar conclusão
  Given a mensagem de confirmação está visível
  When o Rui confirma
  Then o projeto fica "Concluído"

Scenario: cancelar conclusão
  Given a mensagem de confirmação está visível
  When o Rui cancela
  Then o projeto mantém o estado "Ativo"

Scenario: transição não permitida
  Given o projeto está "Ativo"
  When o Rui tenta mudar o estado diretamente para "Arquivado"
  Then a alteração é recusada

Scenario Outline: projeto arquivado fica só de leitura
  Given o projeto "Armazém Sul" está "Arquivado"
  When <utilizador> tenta <operação>
  Then a alteração é recusada

  Examples:
    | utilizador                 | operação                            |
    | o Rui, gestor do projeto   | editar o nome do projeto            |
    | o Rui, gestor do projeto   | criar uma tarefa                    |
    | a Ana, Administrador       | editar uma tarefa                   |
    | a Marta, responsável       | mudar o estado da sua tarefa        |

Scenario: desarquivar
  Given o projeto "Armazém Sul" está "Arquivado"
  When o Rui muda o estado para "Concluído"
  Then o projeto fica "Concluído"
```

As transições permitidas estão em [requirements.md, secção 4.1](requirements.md#41-estados-do-projeto).

---

### US14 Desativar conta

> **Como** Administrador, **quero** desativar a conta de um colaborador que saiu, **para** que deixe de ter acesso sem se perder o histórico.

**Requisitos:** RF02 · **Regras:** RN16, RN18, RN22 · **Segurança:** SEG08, SEG10

```gherkin
Scenario: desativar conta com tarefas
  Given o Pedro tem 2 tarefas abertas e 4 tarefas concluídas
  When a Ana desativa a conta do Pedro
  Then o Pedro não consegue entrar
  And as 2 tarefas abertas ficam "Sem responsável"
  And as 4 tarefas concluídas mantêm o Pedro como responsável

Scenario: sessão aberta
  Given o Pedro tem uma sessão aberta
  When a Ana desativa a conta do Pedro
  And o Pedro tenta abrir qualquer página
  Then o pedido é recusado
  And o Pedro vê a página de entrada

Scenario: desativar o gestor de um projeto
  Given o Rui é gestor do projeto "Novo armazém"
  When a Ana desativa a conta do Rui
  Then o projeto "Novo armazém" fica "Sem gestor"

Scenario: ação registada
  When a Ana desativa a conta do Pedro
  Then o registo de ações administrativas indica a Ana, a desativação da conta do Pedro, a data e a hora

Scenario: último administrador
  Given a Ana é a única conta ativa com papel Administrador
  When tenta desativar a própria conta
  Then a desativação é recusada
  And aparece a mensagem "Tem de existir pelo menos um administrador ativo"
```

---

### US15 Consultar todos os projetos

> **Como** Observador, **quero** ver os projetos e o progresso de todas as equipas sem os poder alterar, **para** acompanhar o estado geral do trabalho.

**Requisitos:** RF03, RF09 · **Segurança:** SEG02 · **Pressuposto:** P13

```gherkin
Scenario: vê todas as equipas
  Given existem projetos nas equipas "Logística Norte" e "Compras"
  And o Carlos tem papel Observador e não pertence a nenhuma equipa
  When abre a lista de projetos
  Then vê os projetos das duas equipas

Scenario Outline: não altera nada
  When o Carlos tenta <operação>
  Then a alteração é recusada

  Examples:
    | operação                         |
    | criar um projeto                 |
    | editar um projeto                |
    | mudar o estado de um projeto     |
    | criar uma tarefa                 |
    | mudar o estado de uma tarefa     |
```

---

### US16 Editar e apagar tarefa

> **Como** Gestor, **quero** editar e apagar tarefas de um projeto que giro, **para** corrigir o planeamento.

**Requisitos:** RF12 · **Regras:** RN11, RN13, RN19

```gherkin
Scenario: editar
  When o Rui altera o título e o prazo de uma tarefa com valores válidos
  Then as alterações ficam guardadas

Scenario: prazo mudado para o passado
  Given a data atual é 2026-11-10
  When o Rui muda o prazo de uma tarefa para 2026-11-09
  Then a alteração é recusada

Scenario: editar tarefa já atrasada sem mudar o prazo
  Given a data atual é 2026-11-10
  And uma tarefa tem prazo 2026-11-05 e estado "Em curso"
  When o Rui altera apenas o título dessa tarefa
  Then a alteração fica guardada
  And o prazo mantém-se 2026-11-05

Scenario: apagar com confirmação
  Given o projeto tem 10 tarefas, 6 concluídas
  When o Rui apaga uma tarefa aberta e confirma
  Then a tarefa deixa de existir
  And o progresso passa a "66% (6 de 9 concluídas)"

Scenario: cancelar apagar
  When o Rui escolhe apagar uma tarefa e cancela
  Then a tarefa mantém-se
```

---

### US17 Editar projeto

> **Como** Gestor, **quero** editar o nome, a descrição e as datas de um projeto que giro, **para** manter a informação do projeto correta.

**Requisitos:** RF07 · **Regras:** RN05, RN06, RN21, RN22

```gherkin
Scenario: editar dados do projeto
  Given o Rui é gestor do projeto "Novo armazém"
  When muda o nome para "Armazém Norte" e a data de fim prevista para 2027-01-15
  Then as alterações ficam guardadas

Scenario: datas incoerentes
  When o Rui muda a data de fim prevista para uma data anterior à data de início
  Then a alteração é recusada

Scenario: projeto de outro gestor
  Given a Sofia é gestora do projeto "Frota"
  When o Rui tenta editar o projeto "Frota"
  Then a alteração é recusada

Scenario: Gestor tenta mudar o gestor
  When o Rui tenta indicar a Sofia como gestora do projeto "Novo armazém"
  Then a alteração é recusada

Scenario: Administrador muda o gestor
  Given a Sofia tem papel Gestor e pertence à equipa "Logística Norte"
  When a Ana indica a Sofia como gestora do projeto "Novo armazém"
  Then a gestora do projeto passa a ser a Sofia

Scenario Outline: gestor inválido
  When a Ana indica <pessoa> como gestor do projeto "Novo armazém"
  Then a alteração é recusada

  Examples:
    | pessoa                                         |
    | o Tiago (Gestor, não pertence à equipa)        |
    | o Pedro (Membro, pertence à equipa)            |

Scenario: projeto sem gestor
  Given o projeto "Novo armazém" está "Sem gestor"
  When a Ana indica a Sofia como gestora
  Then a gestora do projeto passa a ser a Sofia
  And a Sofia pode editar o projeto

Scenario: mudar a equipa
  When a Ana tenta mudar a equipa do projeto "Novo armazém"
  Then a alteração é recusada
```

---

### US18 Mudar papel de uma conta

> **Como** Administrador, **quero** mudar o papel de uma conta, **para** acompanhar as mudanças de funções dos colaboradores.

**Requisitos:** RF02 · **Regras:** RN18, RN22, RN23 · **Segurança:** SEG10

```gherkin
Scenario: Gestor passa a Membro
  Given o Rui tem papel Gestor e é gestor do projeto "Novo armazém"
  When a Ana muda o papel do Rui para "Membro"
  Then o Rui passa a ter papel Membro
  And o projeto "Novo armazém" fica "Sem gestor"

Scenario: Membro passa a Observador
  Given a Marta tem papel Membro e é responsável por 3 tarefas abertas
  When a Ana muda o papel da Marta para "Observador"
  Then essas 3 tarefas ficam "Sem responsável"

Scenario: ação registada
  When a Ana muda o papel da Marta
  Then o registo de ações administrativas indica a Ana, a conta da Marta, o papel anterior, o papel novo, a data e a hora

Scenario: último administrador
  Given a Ana é a única conta ativa com papel Administrador
  When tenta mudar o próprio papel para "Gestor"
  Then a alteração é recusada
  And aparece a mensagem "Tem de existir pelo menos um administrador ativo"

Scenario: utilizador sem permissão
  When o Rui tenta mudar o papel de outra conta
  Then a alteração é recusada
```

---

## 3. Casos excecionais e casos-limite

Cada caso está coberto por pelo menos um cenário da história indicada.

| # | Situação | Comportamento esperado | História | Regra |
|---|----------|------------------------|----------|-------|
| E1 | Projeto sem tarefas | Mostra "Sem tarefas". Nunca mostra 0% nem dá erro de divisão por zero. | US09 | |
| E2 | Tarefa com prazo igual à data atual | Não está atrasada. | US10 | |
| E3 | Tarefa concluída depois do prazo | Não está atrasada. | US10 | |
| E4 | Tarefa sem prazo | Nunca está atrasada. Aparece no fim de "As minhas tarefas". | US08, US10 | |
| E5 | Responsável desativado com tarefas abertas | Tarefas abertas ficam "Sem responsável". Concluídas mantêm o nome. | US14 | RN16 |
| E6 | Responsável removido da equipa | Tarefas abertas nos projetos dessa equipa ficam "Sem responsável". | US03 | RN17 |
| E7 | Membro tenta alterar tarefa de outra pessoa | Recusado. O estado mantém-se. | US07 | RN15 |
| E8 | Acesso direto a projeto de outra equipa | Recusado. | US12 | |
| E9 | Concluir projeto com tarefas abertas | Pede confirmação com o número de tarefas abertas. | US13 | RN10 |
| E10 | Desativar o último administrador | Recusado. | US14 | RN18 |
| E11 | Criar tarefa em projeto concluído ou arquivado | Recusado. | US05 | RN08 |
| E12 | Reabrir tarefa concluída | Permitido. O progresso desce. | US07 | RN12 |
| E13 | Título com 200 e com 201 caracteres | 200 é aceite, 201 é recusado. | US05 | RN11 |
| E14 | Gestor de um projeto desativado, removido da equipa ou com papel mudado | O projeto fica "Sem gestor". Só o Administrador o altera até indicar novo gestor. | US03, US14, US17, US18 | RN22 |
| E15 | 5 tentativas de entrada falhadas seguidas | A conta fica bloqueada durante 15 minutos, mesmo com a palavra-passe correta. | US01 | SEG09 |
| E16 | Conta desativada com sessão aberta | O pedido seguinte é recusado e a pessoa vê a página de entrada. | US14 | SEG08 |
| E17 | Tarefas com o mesmo prazo em "As minhas tarefas" | Ordenadas por prioridade e depois por título. | US08 | RN24 |
| E18 | Editar outro campo de uma tarefa já atrasada | Permitido. A validação do prazo só se aplica quando o prazo muda. | US16 | RN13 |
| E19 | Atribuir tarefa a um Observador | Recusado. Só papéis Gestor e Membro podem ser responsáveis. | US06 | RN14 |
| E20 | Alterar tarefas num projeto concluído | Recusado. É preciso reabrir o projeto. | US07 | RN09 |

---

## 4. Definição de concluído (por história)

Uma história só está concluída quando:

1. Todos os seus cenários são verificados por testes automáticos que passam (RNF07).
2. As regras de negócio indicadas são respeitadas.
3. O resultado foi revisto por uma pessoa da equipa.
4. Qualquer pressuposto novo ou alteração de âmbito ficou registado em [constraints.md](constraints.md) ou em [requirements.md](requirements.md).
