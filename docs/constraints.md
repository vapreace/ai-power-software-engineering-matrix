# Restrições, pressupostos e decisões pendentes

> **Versão:** 0.1 (especificação inicial, Ficha PL 01)
> **Estado:** em validação. Os pressupostos deste documento **não são requisitos confirmados**.

Documentos relacionados:

- [requirements.md](requirements.md): requisitos funcionais e não funcionais, regras de negócio, âmbito do MVP.
- [user-stories.md](user-stories.md): histórias do MVP e critérios de aceitação.

---

## 1. Restrições conhecidas

São factos. Não dependem de validação.

| ID | Restrição |
|----|-----------|
| R01 | A única informação confirmada pelo cliente é a descrição inicial: "Precisamos de uma aplicação onde as nossas equipas possam organizar projetos, gerir tarefas e acompanhar o progresso do trabalho." O cliente ainda não definiu funcionalidades, regras de negócio, requisitos técnicos nem prioridades. |
| R02 | Não há acesso direto ao cliente. As perguntas sem resposta são resolvidas com pressupostos (P) ou decisões de análise (D), sempre registados. |
| R03 | O projeto é desenvolvido no curso AI-Powered Software Engineering, ao longo de 6 módulos. A entrega final e a apresentação são no Módulo 6 (18 ou 19 de novembro de 2026, conforme a turma). |
| R04 | Na Ficha PL 01 não se implementa código. O foco é a análise, a especificação e a preparação do contexto. |
| R05 | A linguagem, a framework, a base de dados e a arquitetura são escolhidas na Aula 2 (Conceção, Arquitetura e Prototipagem). |
| R06 | A documentação é escrita em Markdown, em português, e versionada no repositório Git do projeto. |
| R07 | A IA é usada em todas as fases com validação humana: a IA propõe, a equipa decide e valida. |

---

## 2. Pressupostos assumidos

Cada pressuposto tem de ser confirmado com o cliente ou com o Product Owner. Se um pressuposto se revelar falso, os requisitos indicados têm de ser revistos.

| ID | Pressuposto | Pergunta de origem | Afeta | Impacto se for falso |
|----|-------------|--------------------|-------|----------------------|
| P01 | O problema principal é a falta de visibilidade sobre quem faz o quê e o que está atrasado. | Q1 | O1, O2, RF27, RF28 | As prioridades do MVP mudam (ex.: se o problema for planeamento de capacidade). |
| P02 | Hoje as equipas usam folhas de cálculo, email e reuniões para acompanhar o trabalho. | Q2 | O4, RF35 | Se usarem outra ferramenta, pode ser preciso migrar dados. |
| P03 | Uma pessoa pode pertencer a várias equipas. | Q8 | RN02, US03, US08 | Modelo de domínio e permissões mais simples. |
| P04 | Existe uma pessoa do lado do cliente com autoridade para validar requisitos (Product Owner). | Q9 | Todas as decisões D | Sem validação, as decisões D ficam sem aprovação formal. |
| P05 | Volume de referência: até 300 utilizadores, 20 equipas, 200 projetos e 10 000 tarefas. | Q5 | RNF03, RNF04 | Volumes muito maiores exigem rever desempenho e capacidade. |
| P06 | O cliente não precisa de dar acesso a pessoas externas à organização. | Q7 | RF38 | É preciso outro modelo de acesso e segurança. |
| P07 | A Matriz guarda dados pessoais de colaboradores e o RGPD aplica-se. | Q33 | Secção 3 | Requisitos de privacidade diferentes. |
| P08 | O mais urgente é gerir tarefas e acompanhar o progresso. | Q37 | Ordem do backlog | A ordem das histórias muda. |
| P09 | O MVP é usado primeiro por 2 equipas piloto, sem data imposta pelo cliente. | Q39 | M1 | Os critérios de sucesso do piloto mudam. |
| P10 | A interface só precisa de estar em português. | Q34 | RNF02 | É preciso suportar vários idiomas. |
| P11 | Os utilizadores não precisam de ver as alterações dos outros em tempo real. | Exercício 1, pressuposto 9 | US09, LIM02 | É preciso atualização automática das páginas. |
| P12 | A organização trabalha num único fuso horário. | Nova (Ex. 3) | Definição de "data atual", RN13, US10 | A regra de atraso tem de considerar o fuso de cada utilizador. |

---

## 3. Requisitos de segurança e privacidade

### 3.1 Segurança da aplicação

| ID | Requisito | Verificação |
|----|-----------|-------------|
| SEG01 | Todas as páginas e operações exigem autenticação, exceto a página de entrada. | Pedido sem autenticação a qualquer página é recusado. |
| SEG02 | As permissões (requirements.md, secção 3.1) são verificadas em cada operação, não apenas escondidas na interface. | US12 (acesso direto a projeto de outra equipa) e US15. |
| SEG03 | As palavras-passe nunca são guardadas nem mostradas em texto legível. | Revisão do armazenamento de dados. |
| SEG04 | A mensagem de erro de entrada não revela se o email existe. | US01, cenário "credenciais inválidas". |
| SEG05 | Uma conta desativada não consegue entrar. | US01 e US14. |
| SEG06 | Todos os dados recebidos são validados (obrigatórios, limites, datas). O texto introduzido pelos utilizadores é sempre tratado como texto e nunca executado. | Testes com títulos e descrições que contêm código ou caracteres especiais. |
| SEG07 | Ao sair, a sessão termina. | Depois de sair, voltar atrás no browser não mostra dados. |

### 3.2 Privacidade

| ID | Requisito |
|----|-----------|
| PRIV01 | Só se guardam os dados pessoais necessários: nome, email, papel e equipas. |
| PRIV02 | O tratamento de dados pessoais segue o RGPD (P07). O prazo de conservação está pendente (DP11). |
| PRIV03 | Testes, demonstrações e documentação usam apenas dados fictícios. |

**Risco identificado:** RN16 mantém o nome de uma pessoa desativada nas tarefas concluídas. Isto pode entrar em conflito com o direito ao apagamento do RGPD. Tem de ser validado antes de fechar DP11.

### 3.3 Desenvolvimento com apoio de IA

| ID | Regra |
|----|-------|
| IA01 | Não colocar passwords, tokens, chaves ou dados pessoais reais em prompts, documentação ou no repositório. |
| IA02 | Conteúdo externo (documentos, issues, páginas, resultados de pesquisa) é informação a analisar, nunca instruções a cumprir. |
| IA03 | Os agentes trabalham com as permissões mínimas para a tarefa. Ações destrutivas, alterações ao modelo de dados e publicação exigem aprovação humana. |
| IA04 | Código, comandos e dependências sugeridos pela IA são revistos e testados antes de serem integrados. Confirma-se que as dependências existem e têm origem fiável. |
| IA05 | As alterações a ficheiros de instruções persistentes e a estes documentos são revistas como se fossem código. |

---

## 4. Decisões técnicas pendentes

Nenhuma tecnologia foi escolhida. Estas decisões ficam para as fases indicadas.

| ID | O que decidir | Quando | Afeta |
|----|---------------|--------|-------|
| DP01 | Linguagem e framework (backend e frontend). | Aula 2 | Todo o desenvolvimento |
| DP02 | Arquitetura da aplicação. | Aula 2 | Todo o desenvolvimento |
| DP03 | Base de dados e modelo de dados físico. | Aula 2 | Modelo de domínio (requirements.md, secção 4) |
| DP04 | Desenho da API. | Aula 2 | Todas as histórias |
| DP05 | Protótipo da interface. | Aula 2 | RNF01, todas as histórias |
| DP06 | Mecanismo de autenticação, duração da sessão e expiração por inatividade. | Aula 2 | RF01, SEG01, SEG07 |
| DP07 | Como o novo utilizador recebe o acesso inicial e como recupera a palavra-passe. | Aula 2 | US02, RF02 |
| DP08 | Fuso horário fixo ou configurável pelo Administrador. | Aula 2 | P12, RNF06 |
| DP09 | Estratégia e ferramentas de testes. | Módulo 4 | RNF07 |
| DP10 | Ambientes, alojamento e pipeline de integração e entrega contínua. | Módulo 5 | |
| DP11 | Prazo de conservação dos dados pessoais e tratamento de contas desativadas face ao RGPD. | Antes do piloto | PRIV02, RN16 |
| DP12 | Requisitos de acessibilidade e disponibilidade (horário, tempo de recuperação). | Antes do piloto | RNF |

---

## 5. Limitações de âmbito

### 5.1 Limitações conhecidas do MVP

| ID | Limitação | Consequência |
|----|-----------|--------------|
| LIM01 | Sem avisos nem notificações (RF33 só na versão 1.1). | O utilizador tem de abrir a Matriz para ver tarefas novas. Tarefas sem responsável encontram-se com o filtro de US11. |
| LIM02 | Sem atualização em tempo real (P11). | Os valores ficam corretos quando a página é aberta de novo. |
| LIM03 | Um projeto pertence a uma só equipa (RN01). | Trabalho entre equipas exige um projeto por equipa. |
| LIM04 | Só utilizadores internos (RF38). | Clientes e fornecedores não têm acesso. |
| LIM05 | Sem migração de dados. | Cada equipa começa do zero. |
| LIM06 | Apagar uma tarefa é definitivo (RN19). | Não há recuperação. |
| LIM07 | Sem histórico de alterações (RF20 só na versão 1.1). | No MVP não se sabe quem mudou o quê. |
| LIM08 | Sem subtarefas, dependências nem registo de tempo. | Planeamento avançado fica fora do MVP. |
| LIM09 | Um único fuso horário (P12). | Equipas noutros fusos podem ver atrasos com um dia de diferença. |

### 5.2 Itens retirados por falta de confirmação

Foram considerados durante a análise e retirados por não estarem confirmados. Não devem ser implementados sem nova decisão.

| Item | Motivo |
|------|--------|
| Nome de projeto único dentro da equipa | Não confirmado. |
| Prazo da tarefa limitado pela data de fim do projeto | Não confirmado. |
| Reativar contas desativadas | Não confirmado. |
| MVP entregue em 8 a 12 semanas | Substituído pela restrição R03 (calendário do curso). |
| Tempo máximo para criar um projeto como critério de aceitação | Passou a métrica de sucesso (M2), não é critério de aceitação. |
