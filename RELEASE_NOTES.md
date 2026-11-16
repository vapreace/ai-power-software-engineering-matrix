# Matriz 1.0.0-rc.1 (versão candidata)

Data: 16/11/2026. Tag `v1.0.0-rc.1` (a entrega da Ficha PL 05 é o mesmo commit, tag `ficha-pl05`).

## Visão geral

Primeira versão candidata da Matriz, a aplicação de gestão de projetos e tarefas para equipas desenvolvida no curso AI-Powered Software Engineering. Tem o MVP completo (as 22 histórias de `docs/user-stories.md`) e passa a poder ser construída, testada, documentada e publicada de forma reproduzível: imagem Docker, Docker Compose, pipeline de integração contínua e ambiente publicado.

É uma versão **candidata**: o MVP ainda não foi validado pelo Product Owner. A versão `1.0.0` só sai depois dessa validação (DA41).

## Funcionalidades

| Área | O que faz |
|------|-----------|
| Acesso | Entrar e sair; palavra-passe nova na primeira entrada; permissões por papel (Administrador, Gestor, Membro, Observador) em cada pedido |
| Contas e equipas | O Administrador gere as contas (criar, editar, mudar o papel, desativar, reativar, repor o acesso) e as equipas |
| Projetos | Criar, editar, eliminar, mudar o estado, arquivar; lista com pesquisa e filtro; progresso e tarefas atrasadas |
| Tarefas | Criar, editar, apagar, mudar o estado; vários responsáveis; comentários; filtros; "As minhas tarefas" |
| Painel | Contagens de tarefas por estado, por projeto e no total |

## Novidades desta versão

- Imagem Docker e Docker Compose com a aplicação e o PostgreSQL 18 (DA38).
- Pipeline no GitHub Actions: build, os quatro níveis de testes, cobertura com quality gate, imagem com smoke tests e análise de segurança com o Trivy (DA39).
- Ambiente publicado em **https://matriz-3lgd.onrender.com**, com deploy automático só depois de a pipeline passar (DA40).
- Correções de segurança: concorrência no último Administrador (F5), cookie de sessão com `Secure` (F6), endereço de quem entra atrás do proxy (F13), palavra-passe gerada nos logs (F14) e Content Security Policy.

A lista completa está no [CHANGELOG.md](CHANGELOG.md).

## Requisitos

- Para correr com o Docker Compose: Docker (Docker Desktop no Windows e no macOS) com o Docker Compose.
- Para desenvolver: Java 21, Node 22.22.2 ou mais recente na versão 22, ou 24.15 ou mais recente, e Docker.

## Instalação

```bash
git clone https://github.com/vapreace/ai-power-software-engineering-matrix.git
cd ai-power-software-engineering-matrix
git switch --detach v1.0.0-rc.1
cp .env.example .env            # mudar MATRIZ_DB_PASSWORD
docker compose up -d --build
```

A aplicação fica em http://localhost:8080. As contas de demonstração e a palavra-passe estão no [README](README.md#contas-de-demonstração). A imagem desta versão também fica no GitHub Container Registry, com a tag `1.0.0-rc.1`.

## Verificação desta versão

| Verificação | Resultado |
|-------------|-----------|
| Testes do backend | 423, 0 falhas (291 unitários, que cobrem todas as branches das regras de negócio) |
| Testes do frontend | 4 de regressão e 8 E2E (4 percursos no computador e a 360 px), 0 falhas |
| Pipeline | Aprovada, com a falha controlada do exercício 4 demonstrada num pull request |
| Trivy (imagem, segredos, `Dockerfile`) | 0 vulnerabilidades altas ou críticas, 0 segredos, 0 problemas de configuração |
| Ambiente publicado | Saúde, cabeçalhos de segurança, HSTS e cookies seguros verificados |

Pormenores em [docs/verification.md](docs/verification.md), secção 15.

## Problemas conhecidos e limitações

| Limitação | Consequência |
|-----------|--------------|
| O ambiente publicado usa planos gratuitos e adormece após 15 minutos sem pedidos (LIM17) | O primeiro pedido seguinte demora cerca de um minuto e as sessões perdem-se |
| As contas de demonstração e a palavra-passe são públicas no ambiente publicado | Qualquer pessoa pode alterar os dados de demonstração (fictícios). A reposição está em `docs/deployment.md`, secção 5.3, ainda por executar |
| Não há staging nem produção, nem proteção do ramo `main` (repositório privado com conta gratuita) | O que impede um deploy de uma versão com falhas é o Render só publicar com a pipeline aprovada |
| Riscos aceites F3 (enumeração de identificadores) e F7 (`minimatch` nas ferramentas de desenvolvimento) | Ver `docs/quality-security-review.md` |
| F15: com a base de dados indisponível, a API responde 500 com o corpo genérico do Spring, e não com o Problem Details em português | Só nessa situação; não revela pormenores internos. A correção muda o contrato da API e fica para depois desta versão |
| Por fazer: testes de carga (RNF03, RNF04), E2E noutros browsers além do Chromium, verificação formal de acessibilidade (DP12) | Ver `docs/verification.md` |
| Por decidir antes de um piloto: conformidade com o RGPD (DP11) e disponibilidade (DP12) | Ver `docs/constraints.md`, secção 4 |

## Documentação

[README.md](README.md) (visão geral e como executar), [docs/architecture.md](docs/architecture.md), [openapi.yaml](openapi.yaml), [docs/deployment.md](docs/deployment.md), [docs/troubleshooting.md](docs/troubleshooting.md) e [docs/verification.md](docs/verification.md).
