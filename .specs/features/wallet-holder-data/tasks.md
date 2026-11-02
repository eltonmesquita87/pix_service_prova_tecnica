# Wallet Holder Data Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

---

**Design**: `.specs/features/wallet-holder-data/design.md`
**Status**: Draft

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: none - strong defaults applied (no `AGENTS.md`, `CONTRIBUTING.md` or coverage threshold; `README.md` has no testing standard; samples taken from `WalletTest`, `MoneyTest`, `CreateWalletUseCaseTest`, `BaseIntegrationTest`).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Domain value object / entity | unit (JUnit 5) | All branches; 1:1 to spec ACs; every listed edge case | `src/test/java/com/elton/pixservice/domain/**/*Test.java` | Quick gate |
| Use case | unit (Mockito) | All branches; 1:1 to spec ACs | `src/test/java/com/elton/pixservice/usecase/*Test.java` | Quick gate |
| Controller (web) | unit (`MockMvcBuilders.standaloneSetup` + `GlobalExceptionHandler`, no Docker) | Every route the task changes: happy, edge, error paths | `src/test/java/com/elton/pixservice/infrastructure/web/controller/*Test.java` | Quick gate |
| Exception handler | unit (`MockHttpServletRequest`, `ListAppender`) | Every new handler and branch, including log content | `src/test/java/com/elton/pixservice/infrastructure/exception/*Test.java` | Quick gate |
| DTO with custom (de)serialization | unit (`ObjectMapper`) | Each custom rule and each default kept | `src/test/java/com/elton/pixservice/infrastructure/web/dto/*Test.java` | Quick gate |
| Repository / persistence | integration (`@DataJpaTest` + Testcontainers, tag `docker`) | Key query paths, round trip of new fields, unique and check constraints | `src/test/java/com/elton/pixservice/infrastructure/persistence/**/*Test.java` | Full gate |
| API end to end | integration (`BaseIntegrationTest` + REST Assured, tag `docker`) | Every spec AC reachable over HTTP, concurrency, log content | `src/test/java/com/elton/pixservice/infrastructure/web/controller/*ApiTest.java` | Full gate |
| JPA entity / SQL migration / domain exception / Swagger interface / docs | none | - (build gate only; migration and entity are exercised by the repository test in T8) | - | Build gate |

## Gate Check Commands

> Generated from codebase - confirm before Execute. `MVN` is the Maven bundled with IntelliJ because `mvn` is not on PATH and the repo has no `.mvn/wrapper`:
> `MVN="/d/Users/Elton/AppData/Local/Programs/IntelliJ IDEA 2026.1.4/plugins/maven/lib/maven3/bin/mvn.cmd"`.
> Baseline before any task: 111 tests, 107 pass, 4 skipped (`@Disabled`), 0 failures.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After tasks with unit tests only (no Docker) | `"$MVN" -B -o test -DexcludedGroups=docker` |
| Full | After tasks with integration tests (needs Docker Desktop running) | `"$MVN" -B -o test` |
| Build | After phase completion or tasks without tests | `"$MVN" -B -o verify -DexcludedGroups=docker` (compile, unit tests, package); the last task of the feature runs it without `-DexcludedGroups` |

---

## Execution Plan

Phases are ordered and run sequentially - each phase completes before the next begins, and tasks within a phase execute in order. `Depends on` inside a phase names the previous task, since execution is strictly sequential.

### Phase 1: Domain

```
T1 → T2 → T3 → T4 → T5
```

### Phase 2: Persistence

```
T6 → T7 → T19 → T8
```

### Phase 3: Use case and web

```
T9 → T10 → T11 → T12 → T13 → T14 → T15 → T16
```

### Phase 4: End-to-end and docs

```
T17 → T18
```

---

## Task Breakdown

### T1: Create DadosTitularInvalidosException

**What**: Domain exception with fixed messages and static factories `cpfInvalido()` ("Invalid CPF"), `emailInvalido()` ("Invalid email"), `telefoneInvalido()` ("Invalid phone"), `nomeInvalido()` ("Invalid name"); none interpolates the value.
**Where**: `src/main/java/com/elton/pixservice/domain/exception/DadosTitularInvalidosException.java`
**Depends on**: None
**Reuses**: `DomainException`, pattern of `ChavePixInvalidaException` (without the interpolated value)
**Requirement**: WHOLD-02, WHOLD-04, WHOLD-06, WHOLD-07, WHOLD-19

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Class extends `DomainException` with the four factories and the exact messages above
- [x] Gate check passes: `"$MVN" -B -o verify -DexcludedGroups=docker`
- [x] Test count: 107 pass, 4 skipped, no deletions

**Tests**: none
**Gate**: build
**Status**: ✅ Done

**Commit**: `feat(wallet): add DadosTitularInvalidosException`

---

### T2: Create CpfJaCadastradoException

**What**: Domain exception with the fixed message "CPF already registered" and no constructor argument.
**Where**: `src/main/java/com/elton/pixservice/domain/exception/CpfJaCadastradoException.java`
**Depends on**: T1
**Reuses**: `DomainException`, pattern of `ChavePixJaRegistradaException`
**Requirement**: WHOLD-10, WHOLD-19

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Class extends `DomainException`, message is exactly "CPF already registered"
- [x] Gate check passes: `"$MVN" -B -o verify -DexcludedGroups=docker`
- [x] Test count: 107 pass, 4 skipped, no deletions

**Tests**: none
**Gate**: build
**Status**: ✅ Done

**Commit**: `feat(wallet): add CpfJaCadastradoException`

---

### T3: Create Cpf value object

**What**: Immutable `Cpf` with `of(String)`, `restore(String)`, `getDigits()`, `masked()` and a masked `toString()`.
**Where**: `src/main/java/com/elton/pixservice/domain/valueobject/Cpf.java`
**Depends on**: T2
**Reuses**: `Money` (factory pattern), `DadosTitularInvalidosException`
**Requirement**: WHOLD-02, WHOLD-03, WHOLD-16, WHOLD-18

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `of` accepts 11 digits and `###.###.###-##`, stores only the 11 digits (`12345678909` and `123.456.789-09` give equal digits)
- [x] `of` throws `DadosTitularInvalidosException` ("Invalid CPF") for: wrong check digit, 10 and 12 digits, letters, spaces inside, other separators (`123 456 789 09`), leading or trailing space, all-equal digits (`00000000000` to `99999999999`), empty and blank string
- [x] `masked()` returns `***.456.789-**` for `12345678909`; `toString()` returns the same masked value
- [x] `restore(digits)` does not revalidate
- [x] `CpfTest` written (>= 12 tests, each `@DisplayName` cites its WHOLD id), including a case where a mutated check digit makes `of` fail
- [x] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [x] Test count: 107 existing pass + new tests pass, no deletions

**Tests**: unit
**Gate**: quick
**Status**: ✅ Done

**Commit**: `feat(wallet): add Cpf value object with validation and masking`

---

### T4: Create WalletHolder value object

**What**: `WalletHolder.of(cpf, nome, email, telefone)` validating and normalizing each optional field, plus `empty()`, getters, public all-args constructor and `@ToString.Exclude` on `nome`, `email`, `telefone`.
**Where**: `src/main/java/com/elton/pixservice/domain/valueobject/WalletHolder.java`
**Depends on**: T3
**Reuses**: `Cpf`, e-mail regex of `PixKey.validateEmail`
**Requirement**: WHOLD-01, WHOLD-03, WHOLD-04, WHOLD-05, WHOLD-06, WHOLD-07, WHOLD-08, WHOLD-09, WHOLD-18

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] All-null input gives an empty holder; any subset of valid fields is kept
- [x] E-mail: trimmed and lowercased on success; "Invalid email" for bad format, 255 chars (254 passes), blank
- [x] Phone: `+5511999999999` passes unchanged; "Invalid phone" for missing `+`, leading `+0`, 7 and 16 digits (8 and 15 pass), letters, leading space, blank
- [x] Name: trimmed on success; "Invalid name" for 1 char, 151 chars (2 and 150 pass), blank
- [x] Blank string in any of the four fields is invalid, `null` is absent (WHOLD-09)
- [x] CPF delegates to `Cpf.of`; `toString()` of a holder with all fields contains none of the CPF digits, e-mail, phone or name
- [x] `WalletHolderTest` written (>= 25 tests, each `@DisplayName` cites its WHOLD id)
- [x] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [x] Test count: previous total + new tests pass, no deletions

**Tests**: unit
**Gate**: quick
**Status**: ✅ Done

**Commit**: `feat(wallet): add WalletHolder value object`

---

### T5: Add holder to Wallet entity

**What**: `Wallet` gets `@Builder.Default @ToString.Exclude private WalletHolder holder = WalletHolder.empty();`.
**Where**: `src/main/java/com/elton/pixservice/domain/entity/Wallet.java`
**Depends on**: T4
**Reuses**: existing Lombok setup of `Wallet`
**Requirement**: WHOLD-13, WHOLD-18

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `Wallet.builder().build().getHolder()` is never null and is empty
- [x] A wallet built with a holder exposes it; `toString()` contains no CPF, e-mail, phone or name
- [x] New cases added to `WalletTest`; its 11 existing tests are unchanged and pass
- [x] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [x] Test count: previous total + >= 3 new tests pass, no deletions

**Tests**: unit
**Gate**: quick
**Status**: ✅ Done

**Commit**: `feat(wallet): add holder to Wallet entity`

---

### T6: Add Flyway migration V3

**What**: Migration adding `cpf`, `nome`, `email`, `telefone` (nullable), `ck_wallets_cpf_format` and the partial unique index `uk_wallets_cpf`, exactly as in the design.
**Where**: `src/main/resources/db/migration/V3__add_wallet_holder_data.sql`
**Depends on**: None (phase 1 complete)
**Reuses**: naming and comment style of `V1__create_tables.sql`
**Requirement**: WHOLD-11, WHOLD-12, WHOLD-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] File matches the SQL in the design; no `NOT NULL`, no default, no backfill
- [x] Gate check passes: `"$MVN" -B -o verify -DexcludedGroups=docker` (the migration itself runs against Postgres in T8)
- [x] Test count: previous total pass, no deletions

**Tests**: none
**Gate**: build
**Status**: ✅ Done

**Commit**: `feat(wallet): add migration for wallet holder columns`

---

### T7: Map holder columns in WalletJpaEntity

**What**: Four new columns with the lengths from the design and `@ToString.Exclude` on all four.
**Where**: `src/main/java/com/elton/pixservice/infrastructure/persistence/entity/WalletJpaEntity.java`
**Depends on**: T6
**Reuses**: existing entity and Lombok annotations
**Requirement**: WHOLD-15, WHOLD-18

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Fields `cpf`, `nome`, `email`, `telefone` mapped to columns of the same name, `toString()` excludes them
- [x] Gate check passes: `"$MVN" -B -o verify -DexcludedGroups=docker` (the `validate` check against the schema runs in T8)
- [x] Test count: previous total pass, no deletions

**Tests**: none
**Gate**: build
**Status**: ✅ Done

**Commit**: `feat(wallet): map holder columns in WalletJpaEntity`

---

### T19: Make Spring and Testcontainers tests runnable (added during Execute)

**What**: Configure `maven-surefire-plugin` to drop `org.codehaus.groovy:groovy` and `groovy-xml` (Spring Boot managed 3.0.19) from the test classpath and to pass `-Dapi.version=1.44` to the test JVM. Without it no Spring context test boots: Groovy 3.0.19 conflicts with the explicit 4.0.16 in `pom.xml`, and Testcontainers 1.19.3 gets `Status 400` from Docker Engine 29.9.2. Found by the T8 worker; both problems predate this feature and explain why every HTTP test in the repo is `@Disabled`.
**Where**: `pom.xml`
**Depends on**: T7
**Reuses**: nothing; no file outside the repository is touched
**Requirement**: Enabler for T8 and T17 (no WHOLD id)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `pom.xml` has a `maven-surefire-plugin` entry with `classpathDependencyExcludes` for the two Groovy artifacts and `<argLine>-Dapi.version=1.44</argLine>`; no dependency version changes
- [x] The Full gate with no command-line workaround, `"$MVN" -B -o test`, boots a Spring context against Postgres: `WalletRepositoryIntegrationTest` (present in the working tree, uncommitted until T8) runs 8 tests with 0 errors
- [x] Test count: all tests pass, 4 original skips unchanged, no deletions (222 run, 0 failures, 0 errors, 4 skipped)

**Tests**: none
**Gate**: full
**Status**: ✅ Done

**Commit**: `build(test): make Spring and Testcontainers tests runnable`

---

### T8: Persist holder and add existsByCpf

**What**: `existsByCpf` in the port, the JPA repository and the implementation, plus holder mapping in `toJpaEntity` and `toDomain` (null holder columns map to `WalletHolder.empty()`; CPF read with `Cpf.restore`).
**Where**: `src/main/java/com/elton/pixservice/infrastructure/persistence/repository/WalletRepositoryImpl.java` (also `WalletRepository` port and `WalletJpaRepository`: an interface and its implementation cannot compile apart)
**Depends on**: T19
**Reuses**: existing `toJpaEntity` and `toDomain`
**Requirement**: WHOLD-03, WHOLD-10, WHOLD-11, WHOLD-12, WHOLD-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `WalletRepositoryIntegrationTest` (`@DataJpaTest`, `@AutoConfigureTestDatabase(replace = NONE)`, `@ActiveProfiles("test")`, `@Tag("docker")`) written with these cases:
  - Flyway applies V1 to V3 and Hibernate `validate` passes
  - Round trip of a wallet with all four holder fields
  - Round trip of a wallet without holder (`WalletHolder.empty()`)
  - `existsByCpf` true and false
  - Two wallets with the same CPF throw `DataIntegrityViolationException` naming `uk_wallets_cpf`
  - Several wallets without CPF are accepted
  - A raw insert with `cpf = '123'` is rejected by `ck_wallets_cpf_format`
  - A wallet row inserted without the new columns (legacy shape) is read with an empty holder
- [x] Quick gate still excludes this class: `"$MVN" -B -o test -DexcludedGroups=docker` passes (214 run, 4 skipped)
- [x] Gate check passes: `"$MVN" -B -o test` (Docker Desktop running)
- [x] Test count: previous total + >= 7 integration tests pass, no deletions (222 run, 8 new, 0 failures, 4 skipped)

**Tests**: integration
**Gate**: full
**Status**: ✅ Done

**Commit**: `feat(wallet): persist holder data and add existsByCpf`

---

### T9: Accept holder in CreateWalletUseCase

**What**: `execute(String userId, WalletHolder holder)` rejects a duplicate CPF via `existsByCpf` before saving; `execute(String userId)` is kept and delegates with `WalletHolder.empty()`.
**Where**: `src/main/java/com/elton/pixservice/usecase/CreateWalletUseCase.java`
**Depends on**: None (phase 2 complete)
**Reuses**: existing `execute` flow, `CpfJaCadastradoException`
**Requirement**: WHOLD-01, WHOLD-10, WHOLD-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] With a CPF that exists, throws `CpfJaCadastradoException` and `save` is never called
- [x] With a new CPF, saves a wallet whose holder equals the argument and balance zero, version 0
- [x] Without CPF, `existsByCpf` is never called
- [x] `execute(userId)` saves an empty holder
- [x] Logs never contain the CPF, e-mail or phone (asserted with a `ListAppender`)
- [x] New cases added to `CreateWalletUseCaseTest`; its 3 existing tests are unchanged and pass
- [x] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [x] Test count: previous total + >= 5 new tests pass, no deletions (220 run, 6 new, 4 skipped)

**Tests**: unit
**Gate**: quick
**Status**: ✅ Done

**Commit**: `feat(wallet): accept holder data in CreateWalletUseCase`

---

### T10: Map holder errors in GlobalExceptionHandler

**What**: Handlers for `DadosTitularInvalidosException` (400) and `CpfJaCadastradoException` (409), plus a `uk_wallets_cpf` branch in `handleDataIntegrityViolationException` that returns 409 "CPF already registered" and logs a fixed text without `ex.getMessage()` or stack trace.
**Where**: `src/main/java/com/elton/pixservice/infrastructure/exception/GlobalExceptionHandler.java`
**Depends on**: T9
**Reuses**: `ErrorResponse.builder()` pattern of the existing handlers
**Requirement**: WHOLD-02, WHOLD-04, WHOLD-06, WHOLD-07, WHOLD-09, WHOLD-10, WHOLD-11, WHOLD-18, WHOLD-19

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `GlobalExceptionHandlerTest` written: each of the four `DadosTitularInvalidosException` factories gives 400 with its exact message and `error` "Bad Request"; `CpfJaCadastradoException` gives 409
- [x] A `DataIntegrityViolationException` whose message contains `uk_wallets_cpf` and `Key (cpf)=(12345678909)` gives 409 "CPF already registered"; the response body and every captured log event contain neither `12345678909` nor its masked form
- [x] A `DataIntegrityViolationException` for `uk_pix_key_value` and `uk_scope_key` keeps the current messages (regression)
- [x] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [x] Test count: previous total + >= 8 new tests pass, no deletions (231 run, 11 new, 4 skipped)

**Tests**: unit
**Gate**: quick
**Status**: ✅ Done

**Commit**: `feat(wallet): map holder errors without leaking CPF`

---

### T11: Handle unreadable request bodies

**What**: Handler for `HttpMessageNotReadableException` returning 400 with the fixed message "Malformed request body" and a log that omits `ex.getMessage()`.
**Where**: `src/main/java/com/elton/pixservice/infrastructure/exception/GlobalExceptionHandler.java`
**Depends on**: T10
**Reuses**: `ErrorResponse.builder()` pattern
**Requirement**: Edge case "cpf with wrong JSON type", WHOLD-19

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Handler returns 400, `error` "Bad Request", message "Malformed request body"
- [x] The exception message, which may carry the typed value, appears in neither the body nor the logs
- [x] Cases added to `GlobalExceptionHandlerTest`
- [x] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [x] Test count: previous total + >= 2 new tests pass, no deletions (233 run, 2 new, 4 skipped)

**Tests**: unit
**Gate**: quick
**Status**: ✅ Done

**Commit**: `fix(api): return 400 for unreadable request bodies`

---

### T12: Create StrictStringDeserializer

**What**: Jackson deserializer that accepts only JSON strings (and `null`) and fails on number, boolean, object and array.
**Where**: `src/main/java/com/elton/pixservice/infrastructure/web/dto/StrictStringDeserializer.java`
**Depends on**: T11
**Reuses**: Jackson `StdDeserializer`
**Requirement**: Edge case "cpf with wrong JSON type"

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `"abc"` deserializes to `abc`; JSON `null` gives `null`; `123`, `true`, `{}`, `[]` throw a `JsonMappingException`
- [x] `StrictStringDeserializerTest` written (>= 6 tests) using a real `ObjectMapper`
- [x] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [x] Test count: previous total + >= 6 new tests pass, no deletions (241 run, 8 new, 4 skipped)

**Tests**: unit
**Gate**: quick
**Status**: ✅ Done

**Commit**: `feat(api): add StrictStringDeserializer`

---

### T13: Extend CreateWalletRequest

**What**: Fields `cpf`, `nome`, `email`, `telefone` with `@JsonDeserialize(using = StrictStringDeserializer.class)`, `@ApiModelProperty` describing format and limits, `@ToString.Exclude`, and an explicit `CreateWalletRequest(String userId)` constructor next to the all-args one.
**Where**: `src/main/java/com/elton/pixservice/infrastructure/web/dto/CreateWalletRequest.java`
**Depends on**: T12
**Reuses**: existing DTO and `@NotBlank` on `userId`; no format annotation (AD-001)
**Requirement**: WHOLD-13, WHOLD-14, edge case "cpf with wrong JSON type"

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `{"userId":"u"}` deserializes with the four new fields null; `new CreateWalletRequest("u")` compiles and behaves the same
- [x] `{"userId":"u","cpf":12345678909}` fails deserialization, and so do numeric `nome`, `email`, `telefone`
- [x] `toString()` contains no CPF, e-mail, phone or name
- [x] `CreateWalletRequestTest` written (>= 5 tests)
- [x] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [x] Test count: previous total + >= 5 new tests pass, no deletions (252 run, 11 new, 4 skipped)

**Tests**: unit
**Gate**: quick
**Status**: ✅ Done

**Commit**: `feat(api): add holder fields to CreateWalletRequest`

---

### T14: Extend WalletResponse

**What**: Fields `cpf`, `nome`, `email`, `telefone`, each with `@JsonInclude(JsonInclude.Include.NON_NULL)`.
**Where**: `src/main/java/com/elton/pixservice/infrastructure/web/dto/WalletResponse.java`
**Depends on**: T13
**Reuses**: existing builder DTO
**Requirement**: WHOLD-16, WHOLD-17, WHOLD-20

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] A response built with only `id`, `userId`, `balance` serializes without any of the four keys
- [ ] A response with all four fields serializes them; `id`, `userId`, `balance` still serialize as before
- [ ] `WalletResponseTest` written (>= 3 tests)
- [ ] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [ ] Test count: previous total + >= 3 new tests pass, no deletions

**Tests**: unit
**Gate**: quick

**Commit**: `feat(api): add holder fields to WalletResponse`

---

### T15: Create wallet with holder in WalletController

**What**: `createWallet` builds `WalletHolder.of(...)` from the request, calls `execute(userId, holder)`, and answers with the masked CPF and the normalized `nome`, `email`, `telefone`; deposit and withdraw stay untouched.
**Where**: `src/main/java/com/elton/pixservice/infrastructure/web/controller/WalletController.java`
**Depends on**: T14
**Reuses**: existing response builder, `Money.of` pattern in `deposit`
**Requirement**: WHOLD-01, WHOLD-02, WHOLD-09, WHOLD-10, WHOLD-13, WHOLD-14, WHOLD-16, WHOLD-17, WHOLD-20

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `WalletControllerTest` written with `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())` and mocked use cases:
  - Full valid body: 201, `cpf` is `***.456.789-**`, `nome`, `email`, `telefone` normalized, `balance` 0
  - Only `userId`: 201 and no `cpf`/`nome`/`email`/`telefone` keys
  - Each invalid field (CPF, e-mail, phone, name, blank string in each): 400 with the exact message, use case never called
  - Use case throws `CpfJaCadastradoException`: 409
  - `userId` empty: 400 "User ID is required"
  - `cpf` as JSON number: 400
  - Deposit and withdraw responses carry no holder keys
- [ ] Gate check passes: `"$MVN" -B -o test -DexcludedGroups=docker`
- [ ] Test count: previous total + >= 12 new tests pass, no deletions

**Tests**: unit
**Gate**: quick

**Commit**: `feat(wallet): create wallet with holder data`

---

### T16: Document the contract in WalletControllerApi

**What**: Update `@ApiOperation` notes and add `@ApiResponse` 409 "CPF já cadastrado" for `createWallet`; mention the optional holder fields and the CPF mask.
**Where**: `src/main/java/com/elton/pixservice/infrastructure/web/controller/api/WalletControllerApi.java`
**Depends on**: T15
**Reuses**: existing Swagger annotations
**Requirement**: WHOLD-10, WHOLD-16

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Annotations compile and the 409 response is listed for `createWallet`
- [ ] Gate check passes: `"$MVN" -B -o verify -DexcludedGroups=docker`
- [ ] Test count: previous total pass, no deletions

**Tests**: none
**Gate**: build

**Commit**: `docs(api): document holder fields and 409 on create wallet`

---

### T17: Add WalletHolderApiTest

**What**: End-to-end test of `POST /wallets` against a real Postgres, one method per behavior below.
**Where**: `src/test/java/com/elton/pixservice/infrastructure/web/controller/WalletHolderApiTest.java`
**Depends on**: None (phase 3 complete)
**Reuses**: `BaseIntegrationTest`, REST Assured; each method draws a fresh valid random CPF
**Requirement**: WHOLD-01 to WHOLD-20

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Class is `@Tag("docker")` and not `@Disabled`; the Spring context starts, which proves `BaseIntegrationTest` works
- [ ] Full body: 201, masked CPF in the response, and the database row holds 11 digits, trimmed name, lowercase e-mail, phone unchanged
- [ ] CPF sent as `123.456.789-09` then as `12345678909`: 201 then 409 "CPF already registered", one row
- [ ] 20 concurrent requests (`ExecutorService` and `CountDownLatch`) with the same CPF: exactly one 201, nineteen 409, one row
- [ ] Only `userId`: 201 with `balance` 0.00 and no holder keys; two wallets without CPF both succeed
- [ ] Each invalid field returns 400 with its message and creates no row
- [ ] Deposit and withdraw responses carry no holder keys
- [ ] A Logback `ListAppender` on the root logger captured during the whole class shows no occurrence of any CPF used (digits only or formatted `###.###.###-##`), no e-mail and no phone; the 400 and 409 bodies hold no CPF
- [ ] Gate check passes: `"$MVN" -B -o test` (Docker Desktop running)
- [ ] Test count: previous total + >= 10 integration tests pass, no deletions

**Tests**: integration
**Gate**: full

**Commit**: `test(wallet): add end-to-end tests for wallet holder data`

---

### T18: Update README for the new fields

**What**: Update the create-wallet example and field table in the README with the optional fields, the CPF mask, the 400 and 409 cases.
**Where**: `README.md`
**Depends on**: T17
**Reuses**: current README structure
**Requirement**: WHOLD-01, WHOLD-10, WHOLD-16

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] README shows a request with the four optional fields and a response with the masked CPF
- [ ] Final gate passes with everything: `"$MVN" -B -o verify` (Docker Desktop running)
- [ ] Test count: all tests pass, 4 original skips unchanged, no deletions

**Tests**: none
**Gate**: build

**Commit**: `docs(wallet): document holder fields in README`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 → T2 → T3 → T4 → T5
Phase 2:  T6 → T7 → T19 → T8
Phase 3:  T9 → T10 → T11 → T12 → T13 → T14 → T15 → T16
Phase 4:  T17 → T18
```

Execution is strictly sequential. 19 tasks pack into 3 batches of whole phases: Phase 1 and 2 (9 tasks), Phase 3 (8 tasks), Phase 4 (2 tasks). T19 was added during Execute (the T8 worker found the test environment blocker).

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1 DadosTitularInvalidosException | 1 file | ✅ Granular |
| T2 CpfJaCadastradoException | 1 file | ✅ Granular |
| T3 Cpf | 1 value object | ✅ Granular |
| T4 WalletHolder | 1 value object | ✅ Granular |
| T5 Wallet.holder | 1 field | ✅ Granular |
| T6 V3 migration | 1 file | ✅ Granular |
| T7 WalletJpaEntity | 1 file | ✅ Granular |
| T8 WalletRepositoryImpl | 3 files (port, JPA interface, implementation) | ⚠️ Cohesive: the port cannot compile without the implementation |
| T9 CreateWalletUseCase | 1 file | ✅ Granular |
| T10 GlobalExceptionHandler (domain errors) | 1 file, 3 handlers | ⚠️ Cohesive |
| T11 GlobalExceptionHandler (unreadable body) | 1 handler | ✅ Granular |
| T12 StrictStringDeserializer | 1 class | ✅ Granular |
| T13 CreateWalletRequest | 1 file | ✅ Granular |
| T14 WalletResponse | 1 file | ✅ Granular |
| T15 WalletController | 1 method | ✅ Granular |
| T16 WalletControllerApi | 1 method | ✅ Granular |
| T17 WalletHolderApiTest | 1 test class | ✅ Granular |
| T18 README | 1 file | ✅ Granular |

## Diagram-Definition Cross-Check

Each task's `Depends on` is the previous task in its phase chain, or `None` for the first task of a phase (the phase order carries that dependency). No task depends on a later phase.

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | first of Phase 1 | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | None | first of Phase 2 | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T19 | T7 | T7 → T19 | ✅ Match |
| T8 | T19 | T19 → T8 | ✅ Match |
| T9 | None | first of Phase 3 | ✅ Match |
| T10 | T9 | T9 → T10 | ✅ Match |
| T11 | T10 | T10 → T11 | ✅ Match |
| T12 | T11 | T11 → T12 | ✅ Match |
| T13 | T12 | T12 → T13 | ✅ Match |
| T14 | T13 | T13 → T14 | ✅ Match |
| T15 | T14 | T14 → T15 | ✅ Match |
| T16 | T15 | T15 → T16 | ✅ Match |
| T17 | None | first of Phase 4 | ✅ Match |
| T18 | T17 | T17 → T18 | ✅ Match |

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Domain exception | none | none | ✅ OK |
| T2 | Domain exception | none | none | ✅ OK |
| T3 | Domain value object | unit | unit | ✅ OK |
| T4 | Domain value object | unit | unit | ✅ OK |
| T5 | Domain entity | unit | unit | ✅ OK |
| T6 | SQL migration | none | none | ✅ OK |
| T7 | JPA entity | none | none | ✅ OK |
| T19 | Build configuration (`pom.xml`) | none | none | ✅ OK (verified by the Full gate; see its Done-when) |
| T8 | Repository / persistence | integration | integration | ✅ OK |
| T9 | Use case | unit | unit | ✅ OK |
| T10 | Exception handler | unit | unit | ✅ OK |
| T11 | Exception handler | unit | unit | ✅ OK |
| T12 | DTO (de)serialization | unit | unit | ✅ OK |
| T13 | DTO | unit | unit | ✅ OK |
| T14 | DTO | unit | unit | ✅ OK |
| T15 | Controller | unit | unit | ✅ OK |
| T16 | Swagger interface | none | none | ✅ OK |
| T17 | API end to end | integration | integration | ✅ OK |
| T18 | Docs | none | none | ✅ OK |
