# AGENTS.md

Instruções para agentes de IA neste repositório. Produto e arquitetura detalhados estão em `README.md` e `docs/reverse-engineering/`; este arquivo traz o que um agente precisa para não errar.

## Visão geral

Microserviço de carteira digital com operações Pix: depósito, saque, transferência, chaves Pix e webhooks.

- Java 11, Spring Boot 2.7.18, PostgreSQL 15, Flyway, Maven.
- Pacote base: `com.elton.pixservice`.
- Camadas (dependências apontam para dentro; `domain` e `usecase` não importam `infrastructure`):
  - `domain`: entidades, value objects, exceções e interfaces de repositório.
  - `usecase`: casos de uso, que orquestram domínio e repositórios.
  - `infrastructure`: controllers REST, JPA, DTOs e `GlobalExceptionHandler`.
- Regras de negócio: `docs/reverse-engineering/01-arquitetura-atual.md` (RN-xx) e `02-domain-capability-map.md` (invariantes INV-x).
- Specs de features: `.specs/`.

## Execução

O projeto é executado com **Docker Compose distribuído**: NGINX como load balancer, réplicas do `pix-service` e PostgreSQL, tudo definido em `docker-compose.distributed.yml`.

| Ação | Comando |
|---|---|
| Build da imagem | `docker-compose -f docker-compose.distributed.yml build` |
| Subir com 2 réplicas | `docker-compose -f docker-compose.distributed.yml up -d --scale pix-service=2` |
| Status | `docker-compose -f docker-compose.distributed.yml ps` |
| Logs da aplicação | `docker-compose -f docker-compose.distributed.yml logs -f pix-service` |
| Derrubar | `docker-compose -f docker-compose.distributed.yml down` |

- Não use `mvn spring-boot:run` nem `docker-compose.local.yml` para executar o projeto: não são o cenário de referência.
- Não suba `docker-compose.local.yml` junto com o distribuído: os dois definem o container `pix-postgres`.
- O profile `monitoring` (Prometheus e Grafana) não funciona hoje: monta arquivos de `./monitoring/`, que não existe no repositório.

## Build e testes

| Tarefa | Comando |
|---|---|
| Compilar e testar | `./mvnw clean test` (no PowerShell: `mvnw.cmd clean test`) |
| Compilar sem testes | `./mvnw clean package -DskipTests` |

- Testes de integração exigem Docker ativo: usam Testcontainers pela URL `jdbc:tc:postgresql:15-alpine`, com o perfil `test`.
- O `pom.xml` não define o perfil `integration-tests`, e o CI o chama com `|| echo`. Não considere a integração coberta por essa etapa.

## Portas

| Acesso | Endereço | Origem |
|---|---|---|
| Aplicação (via NGINX) | `http://localhost:8090` | `8090:80` em `docker-compose.distributed.yml` |
| Métricas e Actuator agregados | `http://localhost:8091` | `8091:8081` em `docker-compose.distributed.yml` |
| Réplica (rede interna do compose) | `8080` (app) e `8081` (actuator) | `Dockerfile` (`SERVER_PORT`, `MANAGEMENT_SERVER_PORT`) |

- Use `http://localhost:8090` para testes manuais, Postman e Gatling. O Gatling (`performance-tests`) já usa `8090`.
- As environments do Postman em `docs/postman/` e `postman/environments/` apontam para `8089` (execução local, fora do compose). Ajuste para `8090` antes de usá-las no cenário distribuído.
- `application.yml` define `8089`; esse valor só vale fora do compose.

## Regras de domínio (não negociáveis)

- **Idempotência**: transferências exigem o header `Idempotency-Key` (`PixController`); webhooks são deduplicados por `eventId` (`WebhookEventRepository.eventAlreadyProcessed`). Não remova nem enfraqueça esses controles.
- **Ledger**: `LedgerEntry` é append-only por convenção (não há método de update). Toda operação que altera saldo grava o lançamento na mesma transação. O banco não impede violações, então teste esse caminho explicitamente.
- **Invariante INV-1**: `wallet.balance` é igual à soma assinada do ledger da carteira. Mudança que altere saldo deve manter essa igualdade.
- **Concorrência**: a carteira é lida com `LockModeType.PESSIMISTIC_WRITE` (`WalletJpaRepository`). Não substitua por leitura sem lock.
- **Dinheiro**: use `BigDecimal` dentro de `Money`. Não crie valor monetário a partir de `double`. `Money.of(double)` existe, mas não o use em código novo.
- **Limites e regras regulatórias** (limites de valor, antifraude, compliance) não estão implementados nem documentados. Não os invente; pergunte.

## Dados pessoais (AD-001 em `.specs/STATE.md`)

- CPF, e-mail, telefone e nome são validados no domínio, com mensagens fixas. Não use `@Pattern` nem outra Bean Validation no DTO para esses campos.
- Não logue `ex.getMessage()` de `MethodArgumentNotValidException` nem de `DataIntegrityViolationException` no `GlobalExceptionHandler`: as duas podem trazer o valor do dado.
- Não devolva CPF em respostas de erro, logs ou mensagens de exceção.

## Banco de dados

- Mudança de schema só por migration Flyway em `src/main/resources/db/migration/`.
- Nunca edite migration já aplicada. A próxima é `V4__<descricao>.sql`, pois a última é `V3__add_wallet_holder_data.sql`.
- Arquivos `*.sql.disabled` não são executados pelo Flyway; ignore-os.
- `ddl-auto: validate`: entidade JPA e migration precisam bater.

## Java 11

- Não use `record`, text blocks, `switch` expression nem pattern matching em `instanceof`. `var` é permitido.
- O CI roda em Java 11 e 17; o alvo é 11.

## Código e testes

- Comportamento novo vem com teste. Unitário: JUnit 5 + Mockito (domínio e use cases). Endpoint: REST Assured, a partir de `BaseIntegrationTest`.
- Não apague nem desabilite teste para o build passar. Se um teste estiver errado, corrija-o e explique o motivo na resposta final.
- Mudança de contrato HTTP (request, response, status ou headers) atualiza a documentação Swagger (`@ApiModelProperty`) e `docs/`.
- Nomes: o domínio usa português (`ChavePixInvalidaException`, `SaldoInsuficienteException`); entidades e infraestrutura usam inglês (`LedgerEntry`, `PixController`). Siga a convenção da área onde a classe vai morar.

## Features e decisões

- Feature nova ou mudança relevante: siga `.specs/features/<feature>/`, na ordem `spec.md` → `design.md` → `tasks.md` → `validation.md`.
- Decisão que afete regra de negócio, dado pessoal ou arquitetura: registre em `.specs/STATE.md` com ID `AD-NNN`.

## Limites

**Sempre**
- Rode `./mvnw clean test` (ou a parte relevante) antes de dar a tarefa por concluída.
- Leia a regra de dado pessoal (AD-001) antes de tocar em DTO, entidade ou logs de erro.

**Pergunte antes de**
- Fazer commit, push, criar branch ou abrir PR.
- Adicionar dependência no `pom.xml` ou mudar a versão de Java ou Spring Boot.
- Mudar um contrato HTTP público.
- Alterar idempotência, locking ou o modelo de ledger.
- Versionar `postman/` ou `docs/postman/`, que hoje não estão no git.

**Nunca**
- Editar migration já aplicada.
- Logar ou devolver CPF, e-mail, telefone ou nome.
- Incluir trailer de atribuição de IA (`Co-Authored-By`, "Generated with") em commits ou PRs.

## Git

- Conventional Commits: `tipo(escopo): descrição curta`, por exemplo `feat(wallet): persist holder data`.
- Tipos usados no histórico: `feat`, `fix`, `docs`, `test`, `build`, `chore`.
- Descrição em inglês, como no histórico recente.
- Um commit por mudança lógica.

## Definição de pronto

- `./mvnw clean test` passa. Se o Docker não estiver disponível, diga que os testes de integração não rodaram.
- Testes novos ou alterados cobrem o comportamento modificado.
- Mudança que afete HTTP, configuração ou infraestrutura foi validada no cenário distribuído (`http://localhost:8090`).
- Documentação, contrato e specs estão atualizados quando a regra ou o contrato mudou.
- A resposta final diz o que foi executado, o que não foi e por quê.
