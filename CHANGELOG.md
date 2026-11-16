# Changelog

Alterações relevantes da Matriz, por versão. O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-PT/1.1.0/) e as versões seguem o [Semantic Versioning](https://semver.org/lang/pt-PT/) (DA41). Enquanto a versão começar por `0` ou tiver sufixo (`-rc.1`), o MVP ainda não foi validado pelo Product Owner.

As versões `0.1.0` e `0.2.0` identificam as entregas das Fichas PL 03 e PL 04, que estão nas tags `ficha-pl03` e `ficha-pl04`; não têm tags de versão próprias.

## [Unreleased]

## [1.0.0-rc.1] - 2026-11-16

Primeira versão candidata: o MVP completo, pronto para ser construído, testado e publicado de forma reproduzível (Ficha PL 05, incremento I9).

### Added

- Imagem Docker da aplicação, num `Dockerfile` multi-stage: o frontend compilado vai dentro do `.jar`; execução com o JRE 21 e um utilizador sem privilégios (DA38).
- Docker Compose com a aplicação e o PostgreSQL 18: rede, volume, healthchecks, restrições de execução, limite de memória e rotação de logs (DA38).
- Endereços de saúde com o Spring Boot Actuator: `/actuator/health/liveness` e `/actuator/health/readiness`, sem pormenores (CT37).
- Logs em JSON (formato ECS) nos containers.
- Pipeline de integração contínua no GitHub Actions, em quatro blocos: build, testes (unitários, integração, frontend e E2E), cobertura com quality gate e entrega (imagem, smoke tests, Trivy e publicação no GitHub Container Registry numa release) (DA39).
- Dependabot para Maven, npm, GitHub Actions e Docker.
- Ambiente publicado no Render, com a base de dados no Neon, em Frankfurt, e deploy automático só com a pipeline aprovada: https://matriz-3lgd.onrender.com (DA40).
- Workflow **Validate published**, que verifica o ambiente publicado.
- Documentação: `docs/deployment.md`, `docs/troubleshooting.md`, este `CHANGELOG.md` e `RELEASE_NOTES.md`; implantação em `docs/architecture.md` (secção 4.1); README com o Docker Compose, a pipeline e o ambiente publicado.
- Testes unitários para todas as branches das regras de negócio (291 testes) e relatório de cobertura com o JaCoCo (passo 2b, depois da Ficha PL 04).
- Testes E2E do percurso do Membro e de todos os percursos num ecrã de 360 px (passo 2b).

### Changed

- `docker compose up -d` passa a arrancar a aplicação completa; para desenvolver com o `mvnw`, arranca-se só a base de dados com `docker compose up -d postgres`. O projeto do Compose chama-se `matriz`.
- O `.jar` passa a ter a versão no nome: `matriz-1.0.0-rc.1.jar`.
- A ligação à base de dados aceita `MATRIZ_DB_SSLMODE` (TLS obrigatório no ambiente publicado).

### Fixed

- Dois Administradores a desativarem-se um ao outro ao mesmo tempo podiam deixar a organização sem Administrador ativo (F5, RN18).
- A página inicial (`/`) era servida sem a Content Security Policy nem os outros cabeçalhos de segurança, por ser um reencaminhamento para o `index.html`.

### Security

- Cookie de sessão com `Secure` no ambiente publicado (F6).
- Endereço de quem faz o pedido lido dos cabeçalhos do proxy no ambiente publicado: o limite de tentativas de entrada deixa de ser partilhado por todos (F13). Com isto, o HSTS e o cookie CSRF também saem com `Secure`.
- O Spring Boot deixa de criar um utilizador em memória e de escrever a sua palavra-passe nos logs (F14).
- Content Security Policy em todas as respostas.
- Análise das dependências e da imagem com o Trivy em cada execução da pipeline: as dependências do backend passam a ser analisadas.

## [0.2.0] - 2026-11-16

Entrega da Ficha PL 04 (tag `ficha-pl04`), com o resto do MVP (incremento I7) e os testes e a revisão de qualidade e segurança (incremento I8).

### Added

- Contas: o Administrador cria, edita, muda o papel, desativa, reativa e repõe o acesso (US02, US14, US18).
- Equipas: criar, mudar o nome, adicionar e remover pessoas (US03).
- Estado do projeto (concluir, reabrir, arquivar, desarquivar) e mudança de gestor (US13, US17).
- "As minhas tarefas" (US08).
- Regras automáticas quando uma pessoa é desativada, sai de uma equipa ou muda de papel.
- Estratégia de testes em três níveis (DA37), plano de testes (`docs/test-plan.md`), testes de integração com verificação na base de dados e testes E2E com Playwright.
- Code Review e Security Review (`docs/quality-security-review.md`), ESLint no frontend.

### Fixed

- O contrato da API dizia que os campos a mais eram recusados nos pedidos de criação; passou a dizer que são ignorados, como a implementação (F4).
- Teste em falta para voltar a adicionar a uma equipa uma pessoa removida (F9).
- Versão do Node no README e em `engines` (F10).

### Removed

- Ecrã provisório "Em breve", que deixou de ser usado (F8).

### Security

- Quem não vê um projeto deixa de descobrir os responsáveis pelas suas tarefas (F1).
- Um Membro deixa de descobrir quem pertence a equipas que não são suas (F2).

## [0.1.0] - 2026-11-16

Entrega da Ficha PL 03 (tag `ficha-pl03`), incrementos I0 a I6.

### Added

- Esqueleto do monólito modular (Spring Boot 4, Vue 3), esquema com Liquibase e dados de demonstração (perfil `demo`).
- Entrar e sair, palavra-passe nova na primeira entrada, permissões por papel em cada pedido (US01).
- Projetos: criar, editar, eliminar, lista com pesquisa e filtro por estado, progresso e atrasos.
- Tarefas: criar, editar, apagar, mudar o estado, vários responsáveis, comentários e filtros.
- Painel inicial com as contagens de tarefas por estado.
- Contrato da API (`openapi.yaml`), testes automáticos e verificação dos casos da ficha (`docs/verification.md`).

[Unreleased]: https://github.com/vapreace/ai-power-software-engineering-matrix/compare/v1.0.0-rc.1...HEAD
[1.0.0-rc.1]: https://github.com/vapreace/ai-power-software-engineering-matrix/compare/ficha-pl04...v1.0.0-rc.1
[0.2.0]: https://github.com/vapreace/ai-power-software-engineering-matrix/compare/ficha-pl03...ficha-pl04
[0.1.0]: https://github.com/vapreace/ai-power-software-engineering-matrix/releases/tag/ficha-pl03
