# Wallet Holder Data Specification

## Problem Statement

`POST /wallets` aceita hoje apenas `userId`. A carteira não guarda quem é o titular (CPF, nome, e-mail, telefone), então não há como identificar a pessoa, evitar duas carteiras para o mesmo CPF nem evoluir para KYC e vínculo com chave Pix. A feature adiciona esses dados de forma opcional e sem quebrar os clientes que enviam só `userId`, tratando o CPF como dado pessoal (LGPD).

## Goals

- [ ] `POST /wallets` aceita e persiste `cpf`, `nome`, `email` e `telefone` opcionais, com validação de formato.
- [ ] Um CPF identifica no máximo uma carteira, inclusive sob requisições concorrentes.
- [ ] Nenhum cliente que envia apenas `userId` muda de comportamento (zero testes existentes alterados para passar).
- [ ] O CPF completo nunca aparece em resposta da API, em log ou em mensagem de erro.

## Out of Scope

Explicitamente excluído para evitar scope creep.

| Feature | Reason |
| ------- | ------ |
| Endpoint `GET /wallets/{id}` e atualização dos dados do titular | O pedido é evoluir a criação; leitura e edição são outra feature |
| Validar CPF contra a Receita Federal ou provedor de KYC | Dependência externa, sem requisito hoje |
| Criptografia do CPF em repouso | Decisão de infraestrutura e segurança separada; esta feature mascara apenas resposta e logs |
| Cruzar o CPF da carteira com chaves Pix do tipo CPF em `RegisterPixKeyUseCase` | Muda regra de chave Pix, fora da criação de carteira |
| Unicidade de e-mail e telefone | Decidido: só o CPF é único |
| Mascarar e-mail e telefone | Decidido: só o CPF é mascarado |
| Direito ao esquecimento, retenção e exclusão (LGPD) | Ciclo de vida de dados é uma feature própria |
| CNPJ e pessoa jurídica | O pedido cita CPF |
| Verificação de posse de e-mail e telefone (OTP) | Sem requisito |

---

## Assumptions & Open Questions

Toda ambiguidade está resolvida ou registrada aqui.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Campos do titular | `cpf`, `nome`, `email`, `telefone` | Escolha do usuário | y |
| Obrigatoriedade | Os 4 campos são opcionais; `userId` continua obrigatório | Escolha do usuário; preserva clientes atuais e o teste de carga Gatling | y |
| Unicidade | Somente `cpf` é único, comparado pelos 11 dígitos | Escolha do usuário | y |
| Privacidade do CPF | Resposta e logs mostram o CPF mascarado `***.456.789-**` (dígitos 4 a 9 visíveis) | Escolha do usuário; o formato vem da opção aceita | y |
| String vazia ou só espaços em campo opcional | Conta como inválida (400), não como ausente | Evita gravar lixo e esconder erro do cliente; `null` ou campo ausente é o único jeito de omitir | n |
| Formato aceito de CPF | Exatamente 11 dígitos ou a máscara `###.###.###-##`; qualquer outro formato é inválido | Mais estrito que `PixKey.validateCpf`, que remove qualquer não-dígito; evita aceitar `123 456 789 09` | n |
| Validade do CPF | Dígitos verificadores corretos e dígitos não todos iguais | Regra padrão do CPF; `00000000000` passa na conta dos dígitos e deve ser rejeitado | n |
| Normalização | CPF gravado só com dígitos; e-mail com trim e minúsculas; nome com trim; telefone como enviado | Garante a comparação de unicidade do CPF e a consulta por e-mail | n |
| Limites de nome | 2 a 150 caracteres após trim | Cabe em `VARCHAR(150)`; um caractere não é nome | n |
| Limites de e-mail | Até 254 caracteres, mesmo padrão regex de `PixKey.validateEmail` | 254 é o máximo do RFC 5321; reaproveita a regra existente | n |
| Formato de telefone | E.164: `+` seguido de 8 a 15 dígitos, primeiro dígito diferente de 0 | Escolha do usuário pelo E.164; os limites seguem a ITU-T E.164 | n |
| Quais respostas trazem dados do titular | Só a resposta de `POST /wallets`; depósito e saque ficam inalterados | Evita espalhar PII em respostas de operações financeiras frequentes | n |
| E-mail e telefone na resposta de criação | Devolvidos já normalizados, sem máscara | Decisão do usuário de mascarar só o CPF | n |
| Linguagem das mensagens de erro | Inglês, como o restante da API | Consistência com `GlobalExceptionHandler` | n |
| Carteiras existentes | Permanecem com os 4 campos nulos; nenhum backfill | Não há fonte para preencher | n |

**Open questions:** none - all resolved or logged above.

---

## User Stories

### P1: Criar carteira com dados do titular ⭐ MVP

**User Story**: Como cliente da API, quero informar CPF, nome, e-mail e telefone ao criar a carteira para que ela identifique o titular.

**Why P1**: É o núcleo do pedido.

**Acceptance Criteria**:
1. WHEN `POST /wallets` recebe `userId` e qualquer subconjunto válido de `cpf`, `nome`, `email` e `telefone` THEN system SHALL responder 201 e persistir todos os campos informados na mesma carteira (WHOLD-01)
2. IF `cpf` é informado e tem formato diferente de 11 dígitos ou `###.###.###-##`, ou dígitos verificadores incorretos, ou todos os dígitos iguais THEN system SHALL responder 400 com a mensagem "Invalid CPF" e não criar a carteira (WHOLD-02)
3. WHEN `cpf` válido é informado com ou sem máscara THEN system SHALL gravar somente os 11 dígitos (WHOLD-03)
4. IF `email` é informado e não corresponde ao padrão `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` ou tem mais de 254 caracteres THEN system SHALL responder 400 com a mensagem "Invalid email" e não criar a carteira (WHOLD-04)
5. WHEN `email` válido é informado THEN system SHALL gravá-lo sem espaços nas pontas e em minúsculas (WHOLD-05)
6. IF `telefone` é informado e não corresponde a `+` seguido de 8 a 15 dígitos com o primeiro diferente de 0 THEN system SHALL responder 400 com a mensagem "Invalid phone" e não criar a carteira (WHOLD-06)
7. IF `nome` é informado e tem menos de 2 ou mais de 150 caracteres após remover espaços nas pontas THEN system SHALL responder 400 com a mensagem "Invalid name" e não criar a carteira (WHOLD-07)
8. WHEN `nome` válido é informado THEN system SHALL gravá-lo sem espaços nas pontas (WHOLD-08)
9. IF `cpf`, `email`, `telefone` ou `nome` é informado como string vazia ou só com espaços THEN system SHALL responder 400 com a mensagem de campo inválido correspondente (WHOLD-09)

**Independent Test**: `POST /wallets` com os 4 campos válidos retorna 201 e o banco contém os valores normalizados; cada campo inválido retorna 400 e `wallets` não ganha linha.

---

### P1: CPF único por carteira ⭐ MVP

**User Story**: Como operador do produto, quero que cada CPF tenha no máximo uma carteira para evitar titulares duplicados.

**Why P1**: Sem unicidade o campo `cpf` não resolve o problema de identificação.

**Acceptance Criteria**:
1. IF o `cpf` informado (comparado pelos 11 dígitos) já pertence a uma carteira THEN system SHALL responder 409 com a mensagem "CPF already registered" e não criar a carteira (WHOLD-10)
2. IF duas requisições concorrentes criam carteiras com o mesmo CPF THEN system SHALL criar exatamente uma carteira, responder 201 a uma requisição e 409 à outra (WHOLD-11)
3. WHEN várias carteiras são criadas sem `cpf` THEN system SHALL aceitar todas, pois ausência de CPF não conta como duplicidade (WHOLD-12)

**Independent Test**: Criar duas carteiras com `12345678909` e `123.456.789-09` retorna 201 e depois 409; 20 requisições paralelas com o mesmo CPF resultam em 1 linha em `wallets`.

---

### P1: Compatibilidade com clientes atuais ⭐ MVP

**User Story**: Como cliente atual da API, quero continuar criando carteira só com `userId` para não precisar mudar minha integração.

**Why P1**: A feature não pode quebrar o que já funciona (testes existentes e Gatling).

**Acceptance Criteria**:
1. WHEN `POST /wallets` recebe apenas `userId` THEN system SHALL responder 201 com saldo 0.00 e os campos do titular ausentes ou nulos, como antes (WHOLD-13)
2. IF `userId` está ausente ou em branco THEN system SHALL responder 400 com a mensagem "User ID is required", como antes (WHOLD-14)
3. WHEN a migração do banco é aplicada THEN system SHALL manter as carteiras existentes com `id`, `user_id`, `balance` e `version` inalterados e os 4 campos novos nulos (WHOLD-15)

**Independent Test**: A suíte de testes existente de criação de carteira e os cenários Gatling continuam passando sem alteração; uma carteira criada antes da migração segue legível.

---

### P2: Proteção do CPF em resposta e logs

**User Story**: Como responsável por privacidade, quero que o CPF completo não vaze pela API nem pelos logs para reduzir a exposição de dado pessoal.

**Why P2**: Exigência LGPD, mas a feature funciona sem ela; entra no mesmo release por ser dado pessoal.

**Acceptance Criteria**:
1. WHEN a carteira é criada com `cpf` THEN system SHALL devolver o CPF mascarado no formato `***.456.789-**` na resposta de `POST /wallets` (WHOLD-16)
2. WHEN a carteira é criada THEN system SHALL devolver `nome`, `email` e `telefone` normalizados na resposta de `POST /wallets` (WHOLD-17)
3. The system SHALL NOT escrever o CPF completo, o e-mail ou o telefone nos logs da aplicação, e o CPF só aparece mascarado (WHOLD-18)
4. The system SHALL NOT incluir o valor do CPF em nenhuma mensagem de erro (WHOLD-19)
5. The system SHALL NOT incluir campos do titular nas respostas de depósito e saque (WHOLD-20)

**Independent Test**: Criar carteira com CPF `12345678909` devolve `***.456.789-**`; buscar `12345678909` nos logs da execução e nos corpos de erro 400 e 409 não encontra ocorrência.

---

## Edge Cases

- IF o JSON traz `cpf` com tipo errado, como número em vez de string, THEN system SHALL responder 400 e não criar a carteira
- IF o `cpf` tem 11 dígitos válidos mas `userId` já existe em outra carteira THEN system SHALL criar a carteira, pois `userId` não é único hoje e continua assim
- WHEN `cpf` é `null` explícito THEN system SHALL tratá-lo como ausente
- IF a gravação falha após a validação THEN system SHALL não deixar carteira parcial, pois a criação é uma única transação

---

## Requirement Traceability

Cada requisito tem um ID único para rastrear design, tasks e validação.

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| WHOLD-01 | P1: Criar carteira com dados do titular | T4, T9, T15, T17 | Implementing |
| WHOLD-02 | P1: Criar carteira com dados do titular | T1, T3, T10, T15, T17 | Implementing |
| WHOLD-03 | P1: Criar carteira com dados do titular | T3, T4, T8, T17 | Implementing |
| WHOLD-04 | P1: Criar carteira com dados do titular | T1, T4, T10, T17 | Implementing |
| WHOLD-05 | P1: Criar carteira com dados do titular | T4, T17 | Implementing |
| WHOLD-06 | P1: Criar carteira com dados do titular | T1, T4, T10, T17 | Implementing |
| WHOLD-07 | P1: Criar carteira com dados do titular | T1, T4, T10, T17 | Implementing |
| WHOLD-08 | P1: Criar carteira com dados do titular | T4, T17 | Implementing |
| WHOLD-09 | P1: Criar carteira com dados do titular | T4, T10, T15, T17 | Implementing |
| WHOLD-10 | P1: CPF único por carteira | T2, T8, T9, T10, T16, T17 | Implementing |
| WHOLD-11 | P1: CPF único por carteira | T6, T8, T10, T17 | Implementing |
| WHOLD-12 | P1: CPF único por carteira | T6, T8, T17 | Implementing |
| WHOLD-13 | P1: Compatibilidade com clientes atuais | T5, T9, T13, T15, T17 | Implementing |
| WHOLD-14 | P1: Compatibilidade com clientes atuais | T13, T15, T17 | Implementing |
| WHOLD-15 | P1: Compatibilidade com clientes atuais | T6, T7, T8, T17 | Implementing |
| WHOLD-16 | P2: Proteção do CPF em resposta e logs | T3, T14, T15, T16, T17 | Implementing |
| WHOLD-17 | P2: Proteção do CPF em resposta e logs | T14, T15, T17 | Implementing |
| WHOLD-18 | P2: Proteção do CPF em resposta e logs | T3, T4, T5, T7, T9, T10, T17 | Implementing |
| WHOLD-19 | P2: Proteção do CPF em resposta e logs | T1, T2, T10, T11, T17 | Implementing |
| WHOLD-20 | P2: Proteção do CPF em resposta e logs | T14, T15, T17 | Implementing |

**Coverage:** 20 total, 20 mapped to tasks, 0 unmapped

---

## Success Criteria

- [ ] Os 20 requisitos WHOLD-01 a WHOLD-20 têm teste que passa e referencia o ID.
- [ ] A suíte existente (`CreateWalletUseCaseTest`, `WalletControllerIntegrationTest`, `WalletControllerRestAssuredTest`, `PixControllerRestAssuredTest`, `PixTransferEndToEndTest`) passa sem alteração de asserção.
- [ ] 20 requisições concorrentes com o mesmo CPF produzem exatamente 1 carteira.
- [ ] Nenhuma ocorrência do CPF completo nos logs nem nos corpos de erro durante a suíte de integração.
