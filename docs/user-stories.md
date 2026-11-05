# User Stories do MVP

> **Versão:** 0.6 (conciliação com o âmbito da Ficha PL 03, exercício 1)
> **Estado:** em validação com o Product Owner.

## Como ler este documento

- Os termos "tarefa aberta", "tarefa atrasada", "progresso", "data atual", "projetos visíveis" e "Sem gestor" têm o significado definido em [requirements.md, secção 2](requirements.md#2-definições).
- Cada história indica os requisitos (RF), as regras de negócio (RN) e os requisitos de segurança (SEG) e as decisões técnicas (DA) que cobre. Em caso de conflito, prevalece [requirements.md](requirements.md).
- Os critérios de aceitação usam o formato Gherkin (Given / When / Then). As palavras-chave ficam em inglês e o conteúdo em português.
- Os nomes usados nos exemplos e os emails são fictícios. Os emails usam o domínio reservado `empresa.example`.
- **"Recusado"** (operação recusada): a operação não altera nada e aparece uma mensagem de erro. Quando o texto da mensagem importa, o cenário indica-o.
- **"Acesso recusado"**: a pessoa não vê os dados pedidos e aparece a mensagem "Não tem acesso a esta página."
- Salvo indicação em contrário, cada tarefa referida num cenário tem um único responsável (RN14 admite vários).
- Salvo indicação em contrário no `Given`, vale a situação de partida das duas tabelas seguintes e a data atual é **2026-11-10**.

### Pessoas de partida

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

### Projetos de partida

| Projeto | Equipa | Gestor | Estado |
|---------|--------|--------|--------|
| Novo armazém | Logística Norte | Rui | Ativo |
| Frota | Logística Norte | Sofia | Ativo |
| Armazém Sul | Logística Norte | Rui | Arquivado |
| Fornecedores | Compras | Tiago | Ativo |

---

## 1. Backlog priorizado

Todas as histórias são **Must** e fazem parte do MVP. As histórias US19 a US22 vêm do âmbito da Ficha PL 03 ([requirements.md, secção 8.2](requirements.md#82-requisitos-incluídos)). A US15 depende da validação do papel Observador ([requirements.md, secção 3](requirements.md#3-tipos-de-utilizador)). A ordem segue as dependências: primeiro o que permite testar o resto, depois o fluxo que entrega valor.

| Ordem | ID | História | Papel | Requisitos | Depende de |
|:-----:|----|----------|-------|------------|------------|
| 1 | US01 | Entrar e sair da aplicação | Todos | RF01 | |
| 2 | US02 | Criar e editar conta | Administrador | RF02, RF03 | US01 |
| 3 | US03 | Criar equipa e gerir quem lhe pertence | Administrador | RF05, RF06 | US02 |
| 4 | US04 | Criar projeto | Gestor, Administrador | RF07 | US03 |
| 5 | US05 | Criar tarefa | Gestor | RF12, RF14 | US04 |
| 6 | US06 | Gerir responsáveis da tarefa | Gestor | RF13 | US05 |
| 7 | US07 | Mudar estado da tarefa | Membro, Gestor | RF15 | US06 |
| 8 | US08 | Ver "As minhas tarefas" | Membro | RF17 | US06 |
| 9 | US09 | Ver progresso do projeto | Gestor | RF27 | US07 |
| 10 | US10 | Identificar tarefas atrasadas | Gestor | RF28 | US05 |
| 11 | US11 | Filtrar tarefas do projeto | Gestor | RF16 | US05 |
| 12 | US12 | Ver projetos das minhas equipas | Membro | RF09, RF03 | US04 |
| 13 | US13 | Mudar estado e arquivar projeto | Gestor | RF08 | US04 |
| 14 | US17 | Editar projeto | Gestor, Administrador | RF07 | US04 |
| 15 | US14 | Desativar e reativar conta | Administrador | RF02 | US06 |
| 16 | US18 | Mudar papel de uma conta | Administrador | RF02 | US17 |
| 17 | US15 | Consultar todos os projetos | Observador | RF03, RF09 | US09 |
| 18 | US16 | Editar e apagar tarefa | Gestor | RF12 | US05 |
| 19 | US19 | Comentar tarefa | Membro, Gestor | RF19 | US05 |
| 20 | US20 | Eliminar projeto | Gestor, Administrador | RF41 | US04 |
| 21 | US21 | Ver o painel inicial | Todos | RF29, RF03 | US09 |
| 22 | US22 | Pesquisar projetos | Todos | RF40, RF03 | US12 |

---

## 2. Histórias e critérios de aceitação

### US01 Entrar e sair da aplicação

> **Como** utilizador, **quero** entrar na Matriz com o meu email e palavra-passe e sair quando terminar, **para** aceder apenas à informação que o meu papel permite.

**Requisitos:** RF01 · **Regras:** RN04 · **Segurança:** SEG04, SEG05, SEG07, SEG09 · **Decisões:** DA10, DA25, DA26

```gherkin
Scenario: credenciais válidas
  Given existe uma conta ativa com o email "marta@empresa.example"
  When a Marta entra com o email e a palavra-passe corretos
  Then fica autenticada e o seu nome fica visível na página

Scenario Outline: credenciais inválidas
  When alguém entra com o email "<email>" e uma palavra-passe errada
  Then a entrada é recusada
  And aparece a mensagem "Email ou palavra-passe incorretos"

  Examples:
    | email                     | situação da conta |
    | marta@empresa.example     | ativa             |
    | pedro@empresa.example     | desativada        |
    | ninguem@empresa.example   | não existe        |

Scenario: conta desativada com palavra-passe correta
  Given a conta "pedro@empresa.example" está desativada
  When o Pedro entra com o email e a palavra-passe corretos
  Then a entrada é recusada
  And aparece a mensagem "Conta desativada. Contacte o administrador."

Scenario: sair
  Given a Marta está autenticada
  When sai da aplicação
  Then vê a página de entrada
  And voltar atrás no browser não mostra dados da Matriz

Scenario: email com maiúsculas
  Given existe uma conta ativa com o email "marta@empresa.example"
  When a Marta entra com o email "Marta@Empresa.example" e a palavra-passe correta
  Then fica autenticada

Scenario: primeira entrada com palavra-passe temporária
  Given a Ana criou a conta "ines@empresa.example" com uma palavra-passe temporária
  When a Inês entra com essa palavra-passe
  Then vê a página para definir uma palavra-passe nova
  And aparece a mensagem "Defina uma palavra-passe nova para continuar."
  And não consegue abrir outras páginas antes de a definir

Scenario Outline: palavra-passe nova
  Given a Inês está na página para definir uma palavra-passe nova
  When define uma palavra-passe com <n> caracteres
  Then <resultado>

  Examples:
    | n   | resultado                                                                   |
    | 11  | aparece a mensagem "A palavra-passe tem de ter entre 12 e 128 caracteres." |
    | 12  | a palavra-passe fica definida e a Inês vê o painel inicial                 |
    | 128 | a palavra-passe fica definida e a Inês vê o painel inicial                 |
    | 129 | aparece a mensagem "A palavra-passe tem de ter entre 12 e 128 caracteres." |

Scenario Outline: tentativas repetidas
  Given foram feitas 5 tentativas falhadas para "<email>" a partir do mesmo endereço nos últimos 15 minutos
  When é feita nova tentativa para esse email a partir desse endereço, mesmo com a palavra-passe correta
  Then a entrada é recusada
  And aparece a mensagem "Demasiadas tentativas. Tente de novo dentro de 15 minutos."

  Examples:
    | email                    |
    | marta@empresa.example    |
    | ninguem@empresa.example  |

Scenario: o bloqueio não afeta outro endereço
  Given foram feitas 5 tentativas falhadas para "marta@empresa.example" a partir de um endereço, nos últimos 15 minutos
  When a Marta entra a partir de outro endereço com a palavra-passe correta
  Then fica autenticada

Scenario: sessão sem atividade
  Given a Marta está autenticada e não faz nenhum pedido há 30 minutos
  When abre uma página
  Then vê a página de entrada
  And aparece a mensagem "A sessão terminou. Entre de novo."
```

O mecanismo de entrada, de sessão e de limite de tentativas está em DA25 e DA26 ([architecture.md](architecture.md#9-decisões-técnicas)).

---

### US02 Criar e editar conta

> **Como** Administrador, **quero** criar contas e corrigir o nome e o email, **para** que cada colaborador tenha acesso à Matriz com dados corretos.

**Requisitos:** RF02, RF03 · **Regras:** RN04, RN28, RN29 · **Segurança:** SEG09 · **Decisões:** DA10, DA25

```gherkin
Scenario: criação com sucesso
  When a Ana cria uma conta com nome "Inês Costa", email "ines@empresa.example", papel "Membro"
    e uma palavra-passe temporária com 12 caracteres
  Then a conta aparece na lista de contas com estado "Ativa"

Scenario: palavra-passe temporária curta
  When a Ana cria uma conta com uma palavra-passe temporária com 11 caracteres
  Then a criação é recusada
  And aparece a mensagem "A palavra-passe tem de ter entre 12 e 128 caracteres."

Scenario: email guardado em minúsculas
  When a Ana cria uma conta com o email "Ines@Empresa.example"
  Then a conta fica com o email "ines@empresa.example"

Scenario: email repetido com maiúsculas diferentes
  When a Ana cria uma conta com o email "MARTA@empresa.example"
  Then a criação é recusada
  And aparece a mensagem "Já existe uma conta com este email"

Scenario Outline: comprimento do nome
  When a Ana cria uma conta com um nome de <n> caracteres
  Then <resultado>

  Examples:
    | n   | resultado            |
    | 100 | a conta é criada     |
    | 101 | a criação é recusada |

Scenario Outline: dados inválidos na criação
  When a Ana cria uma conta com nome "<nome>", email "<email>" e papel "<papel>"
  Then a criação é recusada

  Examples:
    | nome       | email                 | papel  |
    |            | ines@empresa.example  | Membro |
    | Inês Costa |                       | Membro |
    | Inês Costa | ines@empresa.example  |        |
    | Inês Costa | ines.empresa.example  | Membro |

Scenario: email repetido na criação
  When a Ana cria uma conta com o email "marta@empresa.example"
  Then a criação é recusada
  And aparece a mensagem "Já existe uma conta com este email"

Scenario: editar o email
  When a Ana muda o email da Marta para "marta.silva@empresa.example"
  Then a conta da Marta passa a ter esse email
  And a Marta entra com o novo email

Scenario: email repetido na edição
  When a Ana muda o email da Marta para "pedro@empresa.example"
  Then a alteração é recusada
  And aparece a mensagem "Já existe uma conta com este email"

Scenario: utilizador sem permissão
  When o Rui tenta abrir a gestão de contas
  Then o acesso é recusado

Scenario: repor o acesso
  Given o Pedro esqueceu a palavra-passe
  And o par do email do Pedro e do seu endereço está bloqueado por tentativas falhadas
  When a Ana define uma palavra-passe temporária para o Pedro
  Then a palavra-passe antiga deixa de funcionar
  And o Pedro entra com a palavra-passe temporária
  And tem de definir uma palavra-passe nova
```

O papel é um de: Administrador, Gestor, Membro, Observador.

O acesso inicial e a reposição do acesso seguem DA25 ([architecture.md](architecture.md#9-decisões-técnicas)).

---

### US03 Criar equipa e gerir quem lhe pertence

> **Como** Administrador, **quero** criar e editar equipas e adicionar ou remover pessoas, **para** definir quem vê cada projeto.

**Requisitos:** RF05, RF06 · **Regras:** RN02, RN03, RN17, RN22, RN26, RN29 · **Decisões:** DA10

```gherkin
Scenario: criar equipa
  When a Ana cria a equipa "Armazém Central"
  Then a equipa aparece na lista de equipas

Scenario Outline: nome inválido
  When a Ana <operação> com o nome "<nome>"
  Then a operação é recusada

  Examples:
    | operação                                   | nome    |
    | cria uma equipa                            |         |
    | cria uma equipa                            | Compras |
    | muda o nome da equipa "Logística Norte"    | Compras |
    | muda o nome da equipa "Logística Norte"    |         |
    | cria uma equipa                            | compras |
    | cria uma equipa                            | "a" repetido 101 vezes |

Scenario: editar o nome
  When a Ana muda o nome da equipa "Compras" para "Compras e Aprovisionamento"
  Then a equipa aparece na lista com o novo nome
  And o projeto "Fornecedores" continua a pertencer-lhe

Scenario: pessoa em duas equipas
  Then a Marta aparece nas equipas "Logística Norte" e "Compras"

Scenario: papel que não pertence a equipas
  When a Ana tenta adicionar o Carlos, com papel Observador, à equipa "Compras"
  Then a operação é recusada

Scenario: contas desativadas não podem ser adicionadas
  Given a conta do Pedro está desativada
  When a Ana procura pessoas para adicionar à equipa "Compras"
  Then o Pedro não aparece na lista

Scenario: remover pessoa com tarefas abertas
  Given a Marta é responsável por 2 tarefas abertas no projeto "Fornecedores"
  When a Ana remove a Marta da equipa "Compras"
  Then essas 2 tarefas ficam com responsável "Sem responsável"

Scenario: remover o gestor de um projeto
  When a Ana remove o Rui da equipa "Logística Norte"
  Then os projetos "Novo armazém" e "Armazém Sul" ficam "Sem gestor"

Scenario: ver quem pertence à equipa
  When o Rui abre a equipa "Logística Norte"
  Then vê o Rui, a Sofia, a Marta e o Pedro

Scenario: remoção com sessão aberta
  Given o Pedro tem uma sessão aberta
  When a Ana remove o Pedro da equipa "Logística Norte"
  And o Pedro tenta abrir o projeto "Novo armazém", sem sair da aplicação
  Then o acesso é recusado
```

---

### US04 Criar projeto

> **Como** Gestor, **quero** criar um projeto numa equipa a que pertenço, **para** agrupar as tarefas desse trabalho.

**Requisitos:** RF07 · **Regras:** RN01, RN05, RN06, RN07, RN21, RN27, RN29

```gherkin
Scenario: Gestor cria projeto
  When o Rui cria o projeto "Inventário 2026" na equipa "Logística Norte"
    com data de início 2026-11-12 e data de fim prevista 2026-12-18
  Then o projeto é criado com estado "Ativo"
  And o gestor do projeto é o Rui
  And o projeto aparece na lista de projetos da equipa

Scenario Outline: dados em falta ou incoerentes
  When o Rui cria um projeto na equipa "Logística Norte" com nome "<nome>",
    data de início "<início>" e data de fim prevista "<fim>"
  Then a criação é recusada

  Examples:
    | nome            | início     | fim        |
    |                 | 2026-11-12 | 2026-12-18 |
    | Inventário 2026 |            | 2026-12-18 |
    | Inventário 2026 | 2026-12-18 | 2026-11-12 |

Scenario: equipa a que não pertence
  When o Rui tenta criar um projeto na equipa "Compras"
  Then a criação é recusada

Scenario: Membro tenta criar projeto
  When a Marta tenta criar um projeto
  Then a criação é recusada

Scenario: Administrador cria projeto e indica o gestor
  When a Ana cria o projeto "Auditoria" na equipa "Compras" com gestor Tiago
  Then o projeto é criado com estado "Ativo" e gestor Tiago

Scenario: Administrador sem gestor
  When a Ana cria um projeto na equipa "Compras" sem indicar gestor
  Then a criação é recusada

Scenario: equipa sem nenhum Gestor
  Given a equipa "Armazém Central" não tem nenhum Gestor
  When a Ana tenta criar um projeto nessa equipa
  Then a criação é recusada
  And aparece a mensagem "A equipa não tem nenhum Gestor"

Scenario: equipa só com Gestor desativado
  Given o Tiago, único Gestor da equipa "Compras", tem a conta desativada
  When a Ana tenta criar um projeto na equipa "Compras"
  Then a criação é recusada
  And aparece a mensagem "A equipa não tem nenhum Gestor"

Scenario Outline: comprimento dos textos
  When o Rui cria um projeto na equipa "Logística Norte" com <campo> de <n> caracteres
  Then <resultado>

  Examples:
    | campo     | n    | resultado            |
    | nome      | 100  | o projeto é criado   |
    | nome      | 101  | a criação é recusada |
    | descrição | 1000 | o projeto é criado   |
    | descrição | 1001 | a criação é recusada |
```

---

### US05 Criar tarefa

> **Como** Gestor, **quero** criar tarefas num projeto que giro, com título, descrição, prazo e prioridade, **para** dividir o trabalho em partes que se podem acompanhar.

**Requisitos:** RF12, RF14 · **Regras:** RN08, RN11, RN12, RN13, RN20, RN29 · **Segurança:** SEG02

```gherkin
Scenario: criação com sucesso
  When o Rui cria no projeto "Novo armazém" a tarefa "Pedir orçamento de estantes"
    com prazo 2026-11-15 e prioridade "Alta"
  Then a tarefa é criada com estado "Por fazer" e responsável "Sem responsável"
  And o total de tarefas do projeto aumenta em 1

Scenario: prioridade não indicada
  When o Rui cria uma tarefa no projeto "Novo armazém" sem indicar prioridade
  Then a tarefa fica com prioridade "Média"

Scenario: prazo no passado
  When o Rui cria uma tarefa no projeto "Novo armazém" com prazo 2026-11-09
  Then a criação é recusada
  And aparece a mensagem "O prazo não pode ser anterior à data atual"

Scenario Outline: título inválido
  When o Rui cria uma tarefa no projeto "Novo armazém" com o título <valor>
  Then a criação é recusada

  Examples:
    | descrição               | valor                    |
    | vazio                   | ""                       |
    | só espaços              | "   "                    |
    | 201 letras simples      | "a" repetido 201 vezes   |
    | 201 letras acentuadas   | "é" repetido 201 vezes   |

Scenario Outline: título válido
  When o Rui cria uma tarefa no projeto "Novo armazém" com o título <valor>
  Then a tarefa é criada com o título <guardado>

  Examples:
    | descrição               | valor                    | guardado                 |
    | 200 letras simples      | "a" repetido 200 vezes   | "a" repetido 200 vezes   |
    | 200 letras acentuadas   | "é" repetido 200 vezes   | "é" repetido 200 vezes   |
    | espaços nas pontas      | "  Rever contrato  "     | "Rever contrato"         |

Scenario Outline: projeto que não aceita tarefas
  Given o projeto "Armazém Sul" tem estado "<estado>"
  When o Rui tenta criar uma tarefa nesse projeto
  Then a criação é recusada

  Examples:
    | estado    |
    | Concluído |
    | Arquivado |

Scenario: Membro tenta criar tarefa
  When a Marta tenta criar uma tarefa no projeto "Novo armazém"
  Then a criação é recusada

Scenario Outline: comprimento da descrição
  When o Rui cria uma tarefa no projeto "Novo armazém" com uma descrição de <n> caracteres
  Then <resultado>

  Examples:
    | n    | resultado            |
    | 2000 | a tarefa é criada    |
    | 2001 | a criação é recusada |
```

---

### US06 Gerir responsáveis da tarefa

> **Como** Gestor, **quero** associar uma ou várias pessoas a uma tarefa e remover cada associação, **para** que fique claro quem a executa.

**Requisitos:** RF13 · **Regras:** RN14, RN26 · **Segurança:** SEG02

```gherkin
Scenario: associar um responsável
  Given a tarefa "Pedir orçamento de estantes" do projeto "Novo armazém" está "Sem responsável"
  When o Rui associa a Marta à tarefa
  Then a Marta passa a ser responsável pela tarefa
  And a tarefa aparece em "As minhas tarefas" da Marta

Scenario: associar vários responsáveis
  Given a Marta é responsável pela tarefa "Pedir orçamento de estantes"
  When o Rui associa também o Pedro à tarefa
  Then a tarefa tem como responsáveis a Marta e o Pedro
  And a tarefa aparece em "As minhas tarefas" da Marta e do Pedro

Scenario: associar a mesma pessoa duas vezes
  Given a Marta é responsável pela tarefa "Pedir orçamento de estantes"
  When o Rui tenta associar de novo a Marta à tarefa
  Then a associação é recusada
  And a Marta aparece uma só vez como responsável

Scenario Outline: responsável inválido
  Given <situação>
  When o Rui tenta associar <pessoa> a uma tarefa do projeto "Novo armazém"
  Then a associação é recusada

  Examples:
    | pessoa    | situação                                 |
    | o João    | o João não pertence a "Logística Norte"  |
    | o Pedro   | a conta do Pedro está desativada         |

Scenario: remover um de vários responsáveis
  Given a Marta e o Pedro são responsáveis pela tarefa "Pedir orçamento de estantes"
  When o Rui remove a Marta da tarefa
  Then o Pedro continua responsável pela tarefa
  And a tarefa deixa de aparecer em "As minhas tarefas" da Marta

Scenario: remover o último responsável
  Given a Marta é a única responsável pela tarefa "Pedir orçamento de estantes"
  When o Rui remove a Marta da tarefa
  Then a tarefa fica "Sem responsável"
  And deixa de aparecer em "As minhas tarefas" da Marta

Scenario Outline: sem permissão
  When <utilizador> tenta associar o Pedro a uma tarefa do projeto "Novo armazém"
  Then a associação é recusada

  Examples:
    | utilizador              |
    | a Marta (Membro)        |
    | a Sofia (outro Gestor)  |
```

---

### US07 Mudar estado da tarefa

> **Como** Membro, **quero** mudar o estado das tarefas de que sou responsável, **para** que o gestor saiba em que ponto está o trabalho.

**Requisitos:** RF15 · **Regras:** RN09, RN12, RN15 · **Decisões:** DA05

```gherkin
Scenario: avançar o estado
  Given a Marta é responsável pela tarefa "Pedir orçamento de estantes" em "Por fazer"
  When muda o estado para "Em curso"
  Then o estado da tarefa é "Em curso"

Scenario: reabrir tarefa concluída
  Given o projeto "Novo armazém" mostra "70% (7 de 10 concluídas)"
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
  When o Rui muda o estado de uma tarefa da Marta no projeto "Novo armazém" para "Concluída"
  Then o estado é "Concluída"

Scenario: gestor responsável num projeto que não gere
  Given o Rui é responsável por uma tarefa do projeto "Frota"
  When o Rui muda o estado dessa tarefa para "Em curso"
  Then o estado é "Em curso"

Scenario Outline: projeto fechado
  Given o projeto "Novo armazém" tem estado "<estado>"
  And a Marta é responsável por uma tarefa aberta desse projeto
  When a Marta tenta mudar o estado dessa tarefa
  Then a alteração é recusada

  Examples:
    | estado    |
    | Concluído |
    | Arquivado |

Scenario: alteração simultânea
  Given o Rui e a Marta abriram a tarefa "Pedir orçamento de estantes" na mesma versão
  And o Rui mudou o estado para "Concluída"
  When a Marta muda o estado para "Em curso" sem abrir de novo a tarefa
  Then a alteração é recusada
  And aparece a mensagem "Esta tarefa foi alterada por outra pessoa. Abra-a de novo e repita a alteração."
  And o estado continua "Concluída"
```

Entre os três estados da tarefa, qualquer mudança é permitida (RN12).

---

### US08 Ver "As minhas tarefas"

> **Como** Membro, **quero** ver numa só página as tarefas de que sou responsável, de todos os projetos ativos, **para** saber o que tenho para fazer.

**Requisitos:** RF17 · **Regras:** RN24, RN25

```gherkin
Background:
  Given a Marta é responsável por:
    | projeto       | estado da tarefa | quantidade |
    | Novo armazém  | aberta           | 3          |
    | Novo armazém  | Concluída        | 1          |
    | Fornecedores  | aberta           | 2          |
    | Armazém Sul   | aberta           | 1          |

Scenario: por omissão
  When a Marta abre "As minhas tarefas"
  Then vê 5 tarefas
  And cada tarefa mostra projeto, prazo, prioridade e estado

Scenario: incluir concluídas
  When a Marta abre "As minhas tarefas" e ativa o filtro "Incluir concluídas"
  Then vê 6 tarefas

Scenario: projeto concluído
  Given o projeto "Fornecedores" passa a "Concluído"
  When a Marta abre "As minhas tarefas"
  Then vê 3 tarefas
```

Os cenários seguintes não usam o `Background`:

```gherkin
Scenario: ordenação
  Given a Marta tem, no projeto "Novo armazém", as tarefas abertas:
    | título     | prazo      | prioridade |
    | Inventário | 2026-11-20 | Baixa      |
    | Orçamento  | 2026-11-12 | Média      |
    | Transporte | 2026-11-12 | Alta       |
    | Arquivo    | sem prazo  | Alta       |
    | Contrato   | 2026-11-12 | Alta       |
  When abre "As minhas tarefas"
  Then as tarefas aparecem pela ordem: Contrato, Transporte, Orçamento, Inventário, Arquivo

Scenario: ordem alfabética sem maiúsculas nem acentos
  Given a Marta tem três tarefas abertas com prazo 2026-11-12 e prioridade "Média":
    "ótica", "Obras" e "orçamento"
  When abre "As minhas tarefas"
  Then as tarefas aparecem pela ordem: Obras, orçamento, ótica

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
  Given o projeto "Novo armazém" tem 3 tarefas, 2 delas concluídas
  When o Rui abre o projeto
  Then vê "66% (2 de 3 concluídas)"

Scenario: projeto sem tarefas
  Given o projeto "Novo armazém" não tem tarefas
  When o Rui abre o projeto
  Then vê "Sem tarefas" em vez de uma percentagem

Scenario: valor atualizado
  Given o projeto "Novo armazém" mostra "60% (6 de 10 concluídas)"
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
  Given uma tarefa do projeto "Novo armazém" tem prazo "<prazo>" e estado "<estado>"
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
  Given o projeto "Novo armazém" tem exatamente <n> tarefas atrasadas
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

> **Como** Gestor, **quero** filtrar as tarefas de um projeto por responsável, estado, prioridade e atraso, **para** encontrar as tarefas que preciso de acompanhar.

**Requisitos:** RF16

```gherkin
Background:
  Given o projeto "Novo armazém" tem exatamente estas tarefas:
    | título | responsáveis    | estado    | prioridade | prazo      |
    | T1     | Marta           | Por fazer | Alta       | 2026-11-09 |
    | T2     | Marta, Pedro    | Em curso  | Média      | 2026-11-20 |
    | T3     | Marta           | Concluída | Baixa      | 2026-11-05 |
    | T4     | Marta           | Concluída | Média      | 2026-11-08 |
    | T5     | Pedro           | Em curso  | Alta       | 2026-11-08 |
    | T6     | Pedro           | Concluída | Média      | 2026-11-01 |
    | T7     | Sem responsável | Por fazer | Baixa      | 2026-11-15 |
    | T8     | Sem responsável | Por fazer | Média      | sem prazo  |
    | T9     | Sem responsável | Concluída | Média      | 2026-11-03 |
    | T10    | Pedro           | Concluída | Baixa      | 2026-11-02 |

Scenario Outline: filtros
  When o Rui filtra por <filtros>
  Then vê apenas as tarefas <resultado>

  Examples:
    | filtros                                | resultado    |
    | estado "Em curso"                      | T2, T5       |
    | "Atrasadas"                            | T1, T5       |
    | responsável "Marta" e "Atrasadas"      | T1           |
    | "Sem responsável"                      | T7, T8, T9   |
    | prioridade "Alta"                      | T1, T5       |
    | estado "Por fazer" e prioridade "Baixa" | T7          |
    | responsável "Pedro" e estado "Em curso" | T2, T5      |

Scenario: sem resultados
  When o Rui filtra por responsável "Pedro" e estado "Por fazer"
  Then não vê nenhuma tarefa
  And vê a mensagem "Nenhuma tarefa corresponde aos filtros"
```

Vários filtros aplicam-se ao mesmo tempo: a tarefa tem de cumprir todos. O filtro por responsável mostra as tarefas em que essa pessoa é um dos responsáveis.

---

### US12 Ver projetos das minhas equipas

> **Como** Membro, **quero** ver a lista de projetos das minhas equipas, filtrada por estado, **para** encontrar os projetos onde trabalho.

**Requisitos:** RF09, RF03 · **Regras:** RN25, RN26 · **Segurança:** SEG02

```gherkin
Scenario: só projetos das minhas equipas
  When o Pedro abre a lista de projetos
  Then vê os projetos "Novo armazém" e "Frota"
  And cada linha mostra nome, equipa, gestor, estado, data de fim prevista,
    progresso e nº de tarefas atrasadas

Scenario: arquivados escondidos por omissão
  When o Pedro abre a lista de projetos e ativa o filtro de estado "Arquivado"
  Then vê o projeto "Armazém Sul"

Scenario: acesso direto a projeto de outra equipa
  When o Pedro tenta abrir diretamente o endereço do projeto "Fornecedores"
  Then o acesso é recusado

Scenario: Gestor sem equipa
  Given o Tiago foi removido da equipa "Compras" e não pertence a nenhuma equipa
  When abre a lista de projetos
  Then não vê nenhum projeto
  And vê a mensagem "Não há projetos para mostrar"
```

---

### US13 Mudar estado e arquivar projeto

> **Como** Gestor, **quero** mudar o estado de um projeto que giro e arquivá-lo, **para** separar o trabalho ativo do terminado.

**Requisitos:** RF08 · **Regras:** RN07, RN09, RN10

```gherkin
Scenario Outline: concluir com tarefas abertas
  Given o projeto "Novo armazém" tem <n> tarefas abertas
  When o Rui muda o estado para "Concluído"
  Then aparece a mensagem "<mensagem>"

  Examples:
    | n | mensagem                                                     |
    | 1 | Este projeto tem 1 tarefa aberta. Concluir mesmo assim?      |
    | 3 | Este projeto tem 3 tarefas abertas. Concluir mesmo assim?    |

Scenario: confirmar conclusão
  Given a mensagem de confirmação está visível
  When o Rui confirma
  Then o projeto fica "Concluído"

Scenario: cancelar conclusão
  Given a mensagem de confirmação está visível
  When o Rui cancela
  Then o projeto mantém o estado "Ativo"

Scenario: concluir sem tarefas abertas
  Given todas as tarefas do projeto "Novo armazém" estão concluídas
  When o Rui muda o estado para "Concluído"
  Then o projeto fica "Concluído" sem pedir confirmação

Scenario: arquivar
  Given o projeto "Novo armazém" está "Concluído"
  When o Rui muda o estado para "Arquivado"
  Then o projeto fica "Arquivado" sem pedir confirmação
  And deixa de aparecer na lista de projetos por omissão

Scenario: reabrir
  Given o projeto "Novo armazém" está "Concluído"
  When o Rui muda o estado para "Ativo"
  Then o projeto fica "Ativo"
  And volta a aceitar novas tarefas

Scenario: transição não permitida
  When o Rui tenta mudar o estado do projeto "Novo armazém" diretamente para "Arquivado"
  Then a alteração é recusada

Scenario Outline: projeto arquivado fica só de leitura
  When <utilizador> tenta <operação> no projeto "Armazém Sul"
  Then a alteração é recusada

  Examples:
    | utilizador                 | operação                         |
    | o Rui, gestor do projeto   | editar o nome do projeto         |
    | o Rui, gestor do projeto   | criar uma tarefa                 |
    | a Ana, Administrador       | editar uma tarefa                |
    | a Marta, responsável       | mudar o estado da sua tarefa     |

Scenario: desarquivar
  When o Rui muda o estado do projeto "Armazém Sul" para "Concluído"
  Then o projeto fica "Concluído"
```

As transições permitidas estão em [requirements.md, secção 4.1](requirements.md#41-estados-do-projeto).

---

### US17 Editar projeto

> **Como** Gestor, **quero** editar o nome, a descrição e as datas de um projeto que giro, **para** manter a informação do projeto correta.

**Requisitos:** RF07 · **Regras:** RN05, RN06, RN09, RN21, RN22 · **Decisões:** DA05

```gherkin
Scenario: editar dados do projeto
  When o Rui muda o nome do projeto "Novo armazém" para "Armazém Norte"
    e a data de fim prevista para 2027-01-15
  Then as alterações ficam guardadas

Scenario: datas incoerentes
  When o Rui muda a data de fim prevista do projeto "Novo armazém" para uma data anterior à data de início
  Then a alteração é recusada

Scenario: projeto de outro gestor
  When o Rui tenta editar o projeto "Frota"
  Then a alteração é recusada

Scenario: projeto fechado
  Given o projeto "Novo armazém" está "Concluído"
  When o Rui tenta mudar o nome do projeto
  Then a alteração é recusada

Scenario: Gestor tenta mudar o gestor
  When o Rui tenta indicar a Sofia como gestora do projeto "Novo armazém"
  Then a alteração é recusada

Scenario: Administrador muda o gestor
  When a Ana indica a Sofia como gestora do projeto "Novo armazém"
  Then a gestora do projeto passa a ser a Sofia

Scenario: Administrador muda o gestor de um projeto fechado
  When a Ana indica a Sofia como gestora do projeto "Armazém Sul"
  Then a gestora do projeto passa a ser a Sofia
  And o projeto continua "Arquivado"

Scenario Outline: gestor inválido
  When a Ana indica <pessoa> como gestor do projeto "Novo armazém"
  Then a alteração é recusada

  Examples:
    | pessoa                                         |
    | o Tiago (Gestor, não pertence à equipa)        |
    | o Pedro (Membro, pertence à equipa)            |
    | a Sofia (Gestor, pertence à equipa, conta desativada) |

Scenario: projeto sem gestor
  Given o projeto "Novo armazém" está "Sem gestor"
  When a Sofia tenta mudar o nome do projeto
  Then a alteração é recusada

Scenario: responsável num projeto sem gestor
  Given o projeto "Novo armazém" está "Sem gestor"
  And a Marta é responsável por uma tarefa aberta desse projeto
  When a Marta muda o estado dessa tarefa para "Concluída"
  Then o estado é "Concluída"

Scenario: novo gestor
  Given o projeto "Novo armazém" está "Sem gestor"
  When a Ana indica a Sofia como gestora
  And a Sofia muda o nome do projeto para "Armazém Norte"
  Then o nome fica guardado

Scenario: mudar a equipa
  When a Ana tenta mudar a equipa do projeto "Novo armazém"
  Then a alteração é recusada

Scenario: edição simultânea do projeto
  Given a Ana e o Rui abriram o projeto "Novo armazém" na mesma versão
  And a Ana mudou a data de fim prevista e gravou
  When o Rui muda o nome e grava sem abrir de novo o projeto
  Then a alteração é recusada
  And aparece a mensagem "Este projeto foi alterado por outra pessoa. Abra-o de novo e repita a alteração."
```

---

### US14 Desativar e reativar conta

> **Como** Administrador, **quero** desativar a conta de um colaborador que saiu e reativá-la se for preciso, **para** controlar o acesso sem perder o histórico.

**Requisitos:** RF02 · **Regras:** RN09, RN16, RN18, RN22, RN28 · **Segurança:** SEG08

```gherkin
Scenario: desativar conta com tarefas
  Given o Pedro tem 2 tarefas abertas e 4 tarefas concluídas no projeto "Novo armazém"
  When a Ana desativa a conta do Pedro
  Then o Pedro não consegue entrar
  And as 2 tarefas abertas ficam "Sem responsável"
  And as 4 tarefas concluídas mantêm o Pedro como responsável

Scenario: tarefa aberta num projeto arquivado
  Given o Pedro é responsável por uma tarefa aberta do projeto "Armazém Sul"
  When a Ana desativa a conta do Pedro
  Then essa tarefa fica "Sem responsável"

Scenario: sessão aberta
  Given o Pedro tem uma sessão aberta
  When a Ana desativa a conta do Pedro
  And o Pedro tenta abrir qualquer página
  Then o pedido é recusado
  And o Pedro vê a página de entrada

Scenario: desativar o gestor de um projeto
  When a Ana desativa a conta do Rui
  Then os projetos "Novo armazém" e "Armazém Sul" ficam "Sem gestor"

Scenario: reativar conta
  Given a conta do Pedro foi desativada e as suas 2 tarefas abertas ficaram "Sem responsável"
  When a Ana reativa a conta do Pedro
  Then o Pedro volta a conseguir entrar
  And continua com papel Membro e na equipa "Logística Norte"
  And as 2 tarefas continuam "Sem responsável"

Scenario: último administrador
  Given a Ana é a única conta ativa com papel Administrador
  When tenta desativar a própria conta
  Then a desativação é recusada
  And aparece a mensagem "Tem de existir pelo menos um administrador ativo"
```

---

### US18 Mudar papel de uma conta

> **Como** Administrador, **quero** mudar o papel de uma conta, **para** acompanhar as mudanças de funções dos colaboradores.

**Requisitos:** RF02 · **Regras:** RN17, RN18, RN22, RN23, RN26

```gherkin
Scenario: Gestor passa a Membro
  When a Ana muda o papel do Rui para "Membro"
  Then o Rui passa a ter papel Membro
  And os projetos "Novo armazém" e "Armazém Sul" ficam "Sem gestor"

Scenario: Membro passa a Observador
  Given a Marta é responsável por 3 tarefas abertas no projeto "Novo armazém"
  When a Ana muda o papel da Marta para "Observador"
  Then a Marta deixa de pertencer às equipas "Logística Norte" e "Compras"
  And essas 3 tarefas ficam "Sem responsável"

Scenario: último administrador
  Given a Ana é a única conta ativa com papel Administrador
  When tenta mudar o próprio papel para "Gestor"
  Then a alteração é recusada
  And aparece a mensagem "Tem de existir pelo menos um administrador ativo"

Scenario: utilizador sem permissão
  When o Rui tenta mudar o papel de outra conta
  Then a alteração é recusada

Scenario: mudança de papel com sessão aberta
  Given o Rui tem uma sessão aberta
  When a Ana muda o papel do Rui para "Membro"
  And o Rui tenta criar uma tarefa no projeto "Novo armazém", sem sair da aplicação
  Then a criação é recusada
```

---

### US15 Consultar todos os projetos

> **Como** Observador, **quero** ver os projetos e o progresso de todas as equipas sem os poder alterar, **para** acompanhar o estado geral do trabalho.

**Requisitos:** RF03, RF09 · **Segurança:** SEG02 · **Pressuposto:** P13

Esta história depende da validação do papel Observador ([requirements.md, secção 3](requirements.md#3-tipos-de-utilizador)).

```gherkin
Scenario: vê todas as equipas
  When o Carlos abre a lista de projetos
  Then vê os projetos "Novo armazém", "Frota" e "Fornecedores"

Scenario Outline: não altera nada
  When o Carlos tenta <operação>
  Then a alteração é recusada

  Examples:
    | operação                                              |
    | criar um projeto                                      |
    | editar o projeto "Novo armazém"                       |
    | mudar o estado do projeto "Novo armazém"              |
    | criar uma tarefa no projeto "Fornecedores"            |
    | mudar o estado de uma tarefa do projeto "Frota"       |
```

---

### US16 Editar e apagar tarefa

> **Como** Gestor, **quero** editar e apagar tarefas de um projeto que giro, **para** corrigir o planeamento.

**Requisitos:** RF12 · **Regras:** RN11, RN13, RN19, RN32 · **Segurança:** SEG02 · **Decisões:** DA05

```gherkin
Scenario: editar
  Given a tarefa "Pedir orçamento de estantes" do projeto "Novo armazém" tem prazo 2026-11-15
  When o Rui muda o título para "Pedir 3 orçamentos de estantes" e o prazo para 2026-11-20
  Then a tarefa fica com esse título e esse prazo

Scenario: prazo mudado para o passado
  When o Rui muda o prazo de uma tarefa do projeto "Novo armazém" para 2026-11-09
  Then a alteração é recusada

Scenario: editar tarefa já atrasada sem mudar o prazo
  Given uma tarefa do projeto "Novo armazém" tem prazo 2026-11-05 e estado "Em curso"
  When o Rui altera apenas o título dessa tarefa
  Then a alteração fica guardada
  And o prazo mantém-se 2026-11-05

Scenario: retirar o prazo de uma tarefa atrasada
  Given uma tarefa do projeto "Novo armazém" tem prazo 2026-11-05 e estado "Em curso"
  When o Rui retira o prazo
  Then a tarefa fica sem prazo
  And deixa de estar marcada como "Atrasada"

Scenario: apagar com confirmação
  Given o projeto "Novo armazém" tem 10 tarefas, 6 concluídas
  When o Rui apaga uma tarefa aberta e confirma
  Then a tarefa deixa de aparecer na lista do projeto
  And o progresso passa a "66% (6 de 9 concluídas)"

Scenario: cancelar apagar
  When o Rui escolhe apagar uma tarefa e cancela
  Then a tarefa mantém-se

Scenario Outline: sem permissão
  When <utilizador> tenta <operação> do projeto "Novo armazém"
  Then a alteração é recusada

  Examples:
    | utilizador              | operação                              |
    | a Marta (Membro)        | editar uma tarefa                     |
    | a Marta (Membro)        | apagar uma tarefa                     |
    | a Sofia (outro Gestor)  | editar uma tarefa                     |

Scenario: edição simultânea
  Given a Ana e o Rui abriram a tarefa "Pedir orçamento de estantes" na mesma versão
  And a Ana mudou o prazo e gravou
  When o Rui muda o título e grava sem abrir de novo a tarefa
  Then a alteração é recusada
  And aparece a mensagem "Esta tarefa foi alterada por outra pessoa. Abra-a de novo e repita a alteração."

Scenario: tarefa apagada entretanto
  Given o Rui abriu a tarefa "Pedir orçamento de estantes"
  And a Ana apagou essa tarefa
  When o Rui grava uma alteração
  Then a alteração é recusada
  And aparece a mensagem "A tarefa não existe."
```

---

### US19 Comentar tarefa

> **Como** Membro, **quero** comentar uma tarefa e ver os comentários dos outros, **para** combinar o trabalho sem trocar emails.

**Requisitos:** RF19 · **Regras:** RN09, RN30 · **Segurança:** SEG02, SEG06

```gherkin
Scenario: comentar
  Given a data e hora atuais são 2026-11-10 14:30
  When a Marta comenta "Já pedi dois orçamentos." na tarefa "Pedir orçamento de estantes" do projeto "Novo armazém"
  Then o comentário aparece na tarefa com o texto "Já pedi dois orçamentos.", a autora Marta e a data e hora 10/11/2026 14:30

Scenario: ordem dos comentários
  Given a tarefa "Pedir orçamento de estantes" tem um comentário do Rui de 2026-11-09 10:00
  When a Marta comenta a tarefa em 2026-11-10 14:30
  Then os comentários aparecem por esta ordem: Rui, Marta

Scenario: o autor é quem está autenticado
  When a Marta envia um comentário com o Pedro indicado como autor
  Then o comentário fica com a Marta como autora

Scenario: comentários guardados
  Given a Marta comentou a tarefa "Pedir orçamento de estantes"
  When o Rui abre de novo a tarefa
  Then vê o comentário da Marta, com a autora e a data e hora

Scenario: o Observador lê os comentários
  Given a Marta comentou a tarefa "Pedir orçamento de estantes"
  When o Carlos abre a tarefa
  Then vê o comentário da Marta

Scenario Outline: texto inválido
  When a Marta comenta uma tarefa do projeto "Novo armazém" com <texto>
  Then o comentário é recusado

  Examples:
    | texto            |
    | texto vazio      |
    | só espaços       |
    | 2001 caracteres  |

Scenario: texto no limite
  When a Marta comenta uma tarefa do projeto "Novo armazém" com 2000 caracteres
  Then o comentário fica guardado

Scenario: texto com código
  When a Marta comenta "<b>urgente</b>" numa tarefa do projeto "Novo armazém"
  Then o comentário aparece exatamente como "<b>urgente</b>"

Scenario Outline: sem permissão
  When <utilizador> tenta comentar uma tarefa do projeto "Novo armazém"
  Then o comentário é recusado

  Examples:
    | utilizador                                 |
    | o João (não pertence a "Logística Norte")  |
    | o Carlos (Observador)                      |

Scenario: projeto fechado
  When a Marta tenta comentar uma tarefa do projeto "Armazém Sul"
  Then o comentário é recusado
```

---

### US20 Eliminar projeto

> **Como** Gestor, **quero** eliminar um projeto que giro, **para** retirar da lista projetos criados por engano ou que deixaram de interessar.

**Requisitos:** RF41 · **Regras:** RN09, RN22, RN31, RN32 · **Segurança:** SEG02

```gherkin
Scenario: eliminar com confirmação
  Given o projeto "Novo armazém" tem 10 tarefas
  And a Marta é responsável por uma tarefa aberta desse projeto
  When o Rui escolhe eliminar o projeto "Novo armazém"
  Then aparece a confirmação "Eliminar o projeto «Novo armazém» e as suas 10 tarefas? Esta ação não pode ser desfeita."
  When o Rui confirma
  Then o projeto deixa de aparecer na lista de projetos e no painel
  And a tarefa deixa de aparecer em "As minhas tarefas" da Marta

Scenario: cancelar
  When o Rui escolhe eliminar o projeto "Novo armazém" e cancela
  Then o projeto mantém-se, com as suas tarefas

Scenario Outline: texto da confirmação
  Given o projeto "Frota" tem <n> tarefas
  When a Sofia escolhe eliminar o projeto "Frota"
  Then aparece a confirmação "<texto>"

  Examples:
    | n | texto                                                                          |
    | 0 | Eliminar o projeto «Frota»? Esta ação não pode ser desfeita.                    |
    | 1 | Eliminar o projeto «Frota» e a sua tarefa? Esta ação não pode ser desfeita.     |

Scenario: os dados continuam guardados
  Given o Rui eliminou o projeto "Novo armazém"
  Then o projeto, as suas tarefas, os responsáveis e os comentários continuam guardados na base de dados, marcados como eliminados

Scenario: abrir um projeto eliminado
  Given o Rui eliminou o projeto "Novo armazém"
  When a Marta abre o endereço do projeto "Novo armazém"
  Then aparece a mensagem "O projeto não existe."

Scenario: eliminar um projeto arquivado
  When o Rui elimina o projeto "Armazém Sul" e confirma
  Then o projeto deixa de aparecer, mesmo com o filtro de estado "Arquivado"

Scenario Outline: sem permissão
  When <utilizador> tenta eliminar o projeto "Novo armazém"
  Then a operação é recusada
  And o projeto mantém-se

  Examples:
    | utilizador              |
    | a Marta (Membro)        |
    | a Sofia (outro Gestor)  |
    | o Carlos (Observador)   |

Scenario: projeto sem gestor
  Given o projeto "Novo armazém" está "Sem gestor"
  When a Ana elimina o projeto e confirma
  Then o projeto deixa de aparecer
```

---

### US21 Ver o painel inicial

> **Como** Gestor, **quero** ver ao entrar os projetos que acompanho e quantas tarefas há em cada estado, **para** saber logo onde está o trabalho.

**Requisitos:** RF29, RF03 · **Regras:** RN25, RN33 · **Segurança:** SEG02

```gherkin
Background:
  Given os projetos têm estas tarefas:
    | projeto       | Por fazer | Em curso | Concluída |
    | Novo armazém  | 2         | 2        | 6         |
    | Frota         | 1         | 0        | 0         |
    | Armazém Sul   | 0         | 0        | 4         |
    | Fornecedores  | 3         | 1        | 1         |

Scenario: painel do Gestor
  When o Rui entra na aplicação
  Then vê o painel com o seu nome e o papel Gestor
  And vê os projetos "Frota" e "Novo armazém", com as contagens da tabela
  And vê os totais: 3 "Por fazer", 2 "Em curso" e 6 "Concluída"
  And não vê o projeto arquivado "Armazém Sul" nem o projeto "Fornecedores"

Scenario: painel do Observador
  When o Carlos entra na aplicação
  Then vê os projetos "Fornecedores", "Frota" e "Novo armazém"
  And vê os totais: 6 "Por fazer", 3 "Em curso" e 7 "Concluída"

Scenario: abrir o detalhe
  When o Rui escolhe o projeto "Novo armazém" no painel
  Then abre o detalhe do projeto "Novo armazém"

Scenario: contagens atualizadas
  When o Rui cria uma tarefa no projeto "Novo armazém" e volta ao painel
  Then o projeto "Novo armazém" mostra 3 "Por fazer", sem ser preciso recarregar a página

Scenario: sem projetos
  Given o Tiago foi removido da equipa "Compras" e não pertence a nenhuma equipa
  When o Tiago entra na aplicação
  Then vê a mensagem "Não há projetos para mostrar"
```

---

### US22 Pesquisar projetos

> **Como** Membro, **quero** pesquisar projetos pelo nome, **para** encontrar depressa o projeto que procuro.

**Requisitos:** RF40, RF03 · **Regras:** RN25, RN34 · **Segurança:** SEG02

```gherkin
Scenario Outline: pesquisar
  When a Marta pesquisa <texto> na lista de projetos
  Then vê apenas os projetos <resultado>

  Examples:
    | texto        | resultado     |
    | "armazém"    | Novo armazém  |
    | "ARMAZEM"    | Novo armazém  |
    | "  frota  "  | Frota         |
    | "forn"       | Fornecedores  |

Scenario: pesquisa com o filtro de estado
  When a Marta liga o filtro de estado "Arquivado" e pesquisa "armazém"
  Then vê os projetos "Armazém Sul" e "Novo armazém"

Scenario: só projetos visíveis
  When o João pesquisa "armazém"
  Then não vê nenhum projeto
  And vê a mensagem "Nenhum projeto corresponde à pesquisa"

Scenario: pesquisa vazia
  Given a Marta pesquisou "frota"
  When a Marta apaga o texto da pesquisa
  Then vê todos os projetos do filtro de estado
```

---

## 3. Casos excecionais e casos-limite

Cada caso está coberto por pelo menos um cenário da história indicada.

| # | Situação | Comportamento esperado | História | Regra |
|---|----------|------------------------|----------|-------|
| E1 | Projeto sem tarefas | Mostra "Sem tarefas". Nunca mostra 0% nem dá erro de divisão por zero. | US09 | |
| E2 | Tarefa com prazo igual à data atual | Não está atrasada. | US10 | |
| E3 | Tarefa concluída depois do prazo | Não está atrasada. | US10 | |
| E4 | Tarefa sem prazo | Nunca está atrasada. Aparece no fim de "As minhas tarefas". | US08, US10 | RN24 |
| E5 | Responsável desativado com tarefas abertas | Deixa de ser responsável pelas tarefas abertas. As que ficam sem responsáveis ficam "Sem responsável". Concluídas mantêm o nome. | US14 | RN16 |
| E6 | Responsável removido da equipa | Deixa de ser responsável pelas tarefas abertas nos projetos dessa equipa. As que ficam sem responsáveis ficam "Sem responsável". | US03 | RN17 |
| E7 | Membro tenta alterar tarefa de outra pessoa | Recusado. O estado mantém-se. | US07 | RN15 |
| E8 | Acesso direto a projeto de outra equipa | Acesso recusado. | US12 | SEG02 |
| E9 | Concluir projeto com tarefas abertas | Pede confirmação com o número de tarefas abertas, no singular ou no plural. | US13 | RN10 |
| E10 | Desativar ou mudar o papel do último administrador | Recusado. | US14, US18 | RN18 |
| E11 | Criar tarefa em projeto concluído ou arquivado | Recusado. | US05 | RN08 |
| E12 | Reabrir tarefa concluída | Permitido. O progresso desce. | US07 | RN12 |
| E13 | Título com 200 e com 201 caracteres, incluindo letras acentuadas | 200 é aceite, 201 é recusado. | US05 | RN11 |
| E14 | Gestor de um projeto desativado, removido da equipa ou com papel mudado | O projeto fica "Sem gestor". Só o Administrador o edita até indicar novo gestor. | US03, US14, US17, US18 | RN22 |
| E15 | Conta desativada ou inexistente com palavra-passe errada | Mensagem genérica "Email ou palavra-passe incorretos". | US01 | SEG04 |
| E16 | Conta desativada com sessão aberta | O pedido seguinte é recusado e a pessoa vê a página de entrada. | US14 | SEG08 |
| E17 | Tarefas com o mesmo prazo em "As minhas tarefas" | Ordenadas por prioridade, depois por título sem distinguir maiúsculas nem acentos. | US08 | RN24 |
| E18 | Editar outro campo de uma tarefa já atrasada | Permitido. A validação do prazo só se aplica quando o prazo muda. | US16 | RN13 |
| E19 | Adicionar um Observador a uma equipa | Recusado. Só Gestor e Membro pertencem a equipas. | US03 | RN26 |
| E20 | Alterar tarefas num projeto concluído | Recusado. É preciso reabrir o projeto. | US07 | RN09 |
| E21 | Título só com espaços ou com espaços nas pontas | Só espaços é recusado. Os espaços nas pontas são retirados. | US05 | RN11 |
| E22 | Desativar alguém com tarefa aberta num projeto arquivado | A pessoa deixa de ser responsável pela tarefa (exceção a RN09). | US14 | RN09, RN16 |
| E23 | Criar projeto numa equipa sem nenhum Gestor | Recusado. | US04 | RN27 |
| E24 | Responsável num projeto "Sem gestor" | Continua a mudar o estado das suas tarefas. | US17 | RN22 |
| E25 | Arquivar um projeto concluído com tarefas abertas | Não pede confirmação. | US13 | RN10 |
| E26 | Gestor que não pertence a nenhuma equipa | Não vê nenhum projeto. | US12 | RN26 |
| E27 | Retirar o prazo de uma tarefa atrasada | Permitido. Deixa de estar atrasada. | US16 | RN13 |
| E28 | Reativar uma conta | Volta com o mesmo papel e equipas. As tarefas não voltam a ser atribuídas. | US14 | RN28 |
| E29 | Mudar o email para um já usado | Recusado. | US02 | RN04, RN28 |
| E30 | Gravar sobre uma versão antiga | Recusado. Nada muda. | US07, US16, US17 | DA05 |
| E31 | Gravar uma tarefa apagada entretanto | Recusado: "A tarefa não existe." | US16 | DA05, RN19 |
| E32 | Email com maiúsculas diferentes | É a mesma conta: entra e não pode ser repetido. | US01, US02 | RN04, DA10 |
| E33 | Nome de equipa que só difere em maiúsculas | Recusado. | US03 | RN03, DA10 |
| E34 | Texto no limite e acima do limite | No limite é aceite, acima é recusado. | US02, US03, US04, US05 | RN29 |
| E35 | Tentativas falhadas repetidas | Pausa de 15 minutos só para esse email e endereço. Mesma mensagem para emails que não existem. | US01 | SEG09, DA26 |
| E36 | Primeira entrada ou acesso reposto | Tem de definir uma palavra-passe nova. | US01, US02 | DA25 |
| E37 | Sessão sem pedidos há 30 minutos | Termina. A pessoa vê a página de entrada. | US01 | DA26 |
| E38 | Gestor com conta desativada | Não pode ser indicado como gestor nem conta como Gestor da equipa. | US04, US17 | RN21, RN27 |
| E39 | Mudança de papel ou de equipa com sessão aberta | Tem efeito no pedido seguinte, sem ser preciso sair. | US03, US18 | SEG02, RN17, RN23 |
| E40 | Associar a mesma pessoa duas vezes à mesma tarefa | Recusado. A pessoa aparece uma só vez. | US06 | RN14 |
| E41 | Remover um de vários responsáveis | Os outros mantêm-se. A tarefa só fica "Sem responsável" quando não resta nenhum. | US06 | RN14 |
| E42 | Comentário vazio, só com espaços ou com mais de 2000 caracteres | Recusado. Com 2000 é aceite. | US19 | RN30 |
| E43 | Pedido de comentário que indica outro autor | O autor é sempre quem está autenticado. | US19 | RN30, SEG02 |
| E44 | Comentar num projeto concluído ou arquivado | Recusado. | US19 | RN09, RN30 |
| E45 | Eliminar um projeto arquivado | Permitido (exceção a RN09). | US20 | RN31 |
| E46 | Abrir um projeto eliminado | "O projeto não existe." Os dados continuam guardados. | US20 | RN32 |
| E47 | Eliminar um projeto com 0, 1 ou várias tarefas | A confirmação indica o número de tarefas, no singular ou no plural. | US20 | RN31 |
| E48 | Pesquisa sem acentos, com maiúsculas ou com espaços nas pontas | Encontra o projeto. | US22 | RN34 |
| E49 | Pesquisa sem resultados | "Nenhum projeto corresponde à pesquisa". | US22 | RN34 |

---

## 4. Definição de concluído (por história)

Uma história só está concluída quando:

1. Todos os seus cenários são verificados por testes automáticos que passam (RNF07). Enquanto não houver ferramenta de testes (DP09), a verificação é manual e fica descrita.
2. As regras de negócio indicadas são respeitadas.
3. O resultado foi revisto por uma pessoa da equipa. Só essa pessoa marca a história como concluída.
4. Qualquer pressuposto novo ou alteração de âmbito ficou registado em [constraints.md](constraints.md) ou em [requirements.md](requirements.md).
