# STATE

## Decisions

### AD-001
- **Decision**: Dado pessoal (CPF, e-mail, telefone, nome) é validado no domínio com mensagens de erro fixas e nunca por Bean Validation no DTO; nenhum handler loga `ex.getMessage()` de erro que possa carregar esses dados.
- **Reason**: `GlobalExceptionHandler` loga `ex.getMessage()` de `MethodArgumentNotValidException` e `DataIntegrityViolationException`, e ambas trazem o valor do dado (valor rejeitado, `Detail: Key (...)` do Postgres).
- **Trade-off**: Perde a declaração `@Pattern` no DTO e a documentação automática do formato; o formato é descrito em `@ApiModelProperty`.
- **Scope**: Toda feature que receba ou persista dado pessoal; `GlobalExceptionHandler`.
- **Date**: 2026-11-01
- **Status**: active

## Handoff

none
