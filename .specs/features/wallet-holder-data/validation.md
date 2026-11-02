# Wallet Holder Data Validation

**Date**: 2026-11-01
**Spec**: `.specs/features/wallet-holder-data/spec.md`
**Diff range**: `698ff49..4497deb` (feature commits `59bcc1e..4497deb`; branch `feature/wallet-holder-data`)
**Verifier**: independent sub-agent (author != verifier)

**Verdict**: PASS (with 2 non-blocking spec-precision gaps, 1 accepted deviation and 1 pre-existing build break outside the feature, all listed below)

The feature code and tests are correct against the spec. 18 of 20 ACs are fully covered with exact-outcome assertions. WHOLD-11 and WHOLD-18 are covered but carry the user-accepted `SqlExceptionHelper` logging gap. The sensor injected 21 valid mutations: 20 killed, 1 survived and that one is a behavior-equivalent mutant (see Sensor). One thing needs attention before the branch can go through CI: committed HEAD does not compile on a clean checkout (see "Gate Check"). That failure is in files the feature did not touch and it is identical at the pre-feature commit `698ff49`.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 DadosTitularInvalidosException | ✅ Done | `domain/exception/DadosTitularInvalidosException.java`, four fixed messages, none interpolates the value |
| T2 CpfJaCadastradoException | ✅ Done | message "CPF already registered" |
| T3 Cpf | ✅ Done | `Cpf.java:35` validation, `Cpf.java:53` mask; `CpfTest` 167 lines |
| T4 WalletHolder | ✅ Done | `WalletHolder.java:19-21` limits, `:57-60` e-mail normalization; `WalletHolderTest` 323 lines |
| T5 Wallet.holder | ✅ Done | `Wallet.java` `@Builder.Default @ToString.Exclude`; `WalletTest.java:182-239` |
| T6 Migration V3 | ✅ Done | `V3__add_wallet_holder_data.sql`, nullable columns, check constraint, partial unique index |
| T7 WalletJpaEntity | ✅ Done | 4 columns, `@ToString.Exclude` |
| T19 Surefire/Testcontainers enabler | ✅ Done | `pom.xml` surefire block, intentional (Groovy conflict, Docker API pin) |
| T8 Persist holder + existsByCpf | ✅ Done | `WalletRepositoryImpl.java:49-50,63-66,77-81` |
| T9 CreateWalletUseCase | ✅ Done | `CreateWalletUseCase.java:31` pre-check; one-argument overload kept |
| T10 Holder errors in handler | ✅ Done | `GlobalExceptionHandler.java:300-312` `uk_wallets_cpf` branch with fixed log |
| T11 Unreadable body -> 400 | ✅ Done | `GlobalExceptionHandler.java` `handleHttpMessageNotReadable`, fixed message, no `ex.getMessage()` |
| T12 StrictStringDeserializer | ✅ Done | `StrictStringDeserializer.java:25` |
| T13 CreateWalletRequest | ✅ Done | 4 fields, `@JsonDeserialize` + `@ToString.Exclude` on each, one-argument constructor |
| T14 WalletResponse | ✅ Done | `WalletResponse.java:19-27` `NON_NULL` per holder field |
| T15 WalletController | ✅ Done | `WalletController.java:42-55`, only `createWallet` changed |
| T16 WalletControllerApi docs | ✅ Done | 409 response and notes |
| T17 WalletHolderApiTest | ✅ Done | 41 tests, tag `docker`, all green |
| T18 README | ✅ Done | one README hunk (intentional) |

All 19 tasks are marked done in `tasks.md` and each has its commit in `git log 698ff49..HEAD` (19 feature commits plus the spec commit).

---

## Spec-Anchored Acceptance Criteria

Paths are under `src/test/java/com/elton/pixservice/`. API = `infrastructure/web/controller/WalletHolderApiTest.java` (real Postgres, HTTP). CT = `infrastructure/web/controller/WalletControllerTest.java`. GEH = `infrastructure/exception/GlobalExceptionHandlerTest.java`. WH = `domain/valueobject/WalletHolderTest.java`. CPF = `domain/valueobject/CpfTest.java`. REPO = `infrastructure/persistence/repository/WalletRepositoryIntegrationTest.java`.

| AC | Spec-defined outcome | `file:line` + assertion | Result |
| -- | -------------------- | ----------------------- | ------ |
| WHOLD-01 | subset of valid fields: 201, all informed fields persisted on the same wallet | `API:119` `assertEquals(201, response.statusCode())`; `API:130-134` row `cpf/nome/email/telefone/user_id` equal sent values; subset `API:147-158` (row nome/email set, cpf/telefone null); `WH:58-108` each single-field subset; `CT:93-107` captured holder equals `WalletHolder.of(...)`; `CreateWalletUseCaseTest.java:155` `assertEquals(holder, saved.getHolder())` | ✅ PASS |
| WHOLD-02 | wrong format / bad check digit / all equal: 400 "Invalid CPF", no wallet | `API:344-350` status 400 + message "Invalid CPF" + `countByUserId == 0` for the cases at `API:359-366` (wrong digit, 10, 12 digits, letters, `123 456 789 09`, `00000000000`, empty, blank); `CPF:98` `assertEquals("Invalid CPF", ex.getMessage())`; `CPF:52,65` all 9 wrong digits at each check position; `CPF:108-110` `0..9` x11; `CPF:70-91` 19 bad formats incl. leading/trailing space and `\n` | ✅ PASS |
| WHOLD-03 | mask accepted, only 11 digits stored | `API:130` `assertEquals(digits, row.get("cpf"))` after sending the masked form (`API:117`); `API:190-198` masked then digits gives 201/409/1 row; `CPF:31-33`; `REPO:90` row cpf `12345678909` | ✅ PASS |
| WHOLD-04 | bad pattern or > 254 chars: 400 "Invalid email" | `API:369-372` 256 and 255 chars, no `@`, `a@b` all `"Invalid email"` (assert `API:346`); boundary 254 passes: `WH:156-159`; 255 rejected: `WH:164-168`; limit after trim: `WH:174-176` | ✅ PASS |
| WHOLD-05 | trimmed, lowercase | `API:124` `maria.silva@example.com` in response, `API:132` same in the DB row; `WH:134`; `CT:99` | ✅ PASS |
| WHOLD-06 | not `+` 8..15 digits first non-zero: 400 "Invalid phone" | `API:373-379` (no plus, `+0..`, 7, 16 digits, letters, empty, blank) message check `API:346`; unit boundaries 8 and 15 pass `WH:182-185`, 7 and 16 rejected `WH:188-203`; stored unchanged `API:133` | ✅ PASS |
| WHOLD-07 | < 2 or > 150 after trim: 400 "Invalid name" | `API:380-384` (1 char, padded 1 char, 151, empty, blank); boundaries 2 and 150 pass `WH:216-227`; limits after trim `WH:244-245` | ✅ PASS |
| WHOLD-08 | nome trimmed | `API:123` `"Maria Silva"` from `"  Maria Silva  "`; `API:131` row; `WH:212` | ✅ PASS |
| WHOLD-09 | empty or whitespace-only: 400 with the matching message | `API:365-366,371-372,378-379,383-384` for all four fields (message at `API:346`, no row `API:350`); `WH:250-276` incl. `\t`; `WH:278-285` null is absent; `CT:163-166` | ✅ PASS |
| WHOLD-10 | same CPF (compared by 11 digits): 409 "CPF already registered", no new wallet | `API:193-198` 201 then 409, `message == "CPF already registered"`, `error == "Conflict"`, `countByCpf == 1`; `GEH:87`; `CreateWalletUseCaseTest.java:136,139`; `REPO:121-123` | ✅ PASS |
| WHOLD-11 | concurrent duplicates: exactly one 201, the rest 409 | `API:235` `assertEquals(1, created)`, `API:236` `assertEquals(requests - 1, conflicts)`, `API:243` `countByCpf == 1`; 20 threads released by a latch (`API:205-222`); `REPO:137` `uk_wallets_cpf` rejects the second insert; `GEH:107` race branch gives 409 | ✅ PASS on behavior. Status stays Implementing: accepted logging gap (see Accepted deviations) |
| WHOLD-12 | wallets without CPF never conflict | `API:302-306` two 201s, both rows `cpf IS NULL`; `REPO:151-153` three wallets with null cpf | ✅ PASS (the partial index is behavior-equivalent to a plain unique index in Postgres, see sensor M15) |
| WHOLD-13 | userId only: 201, balance 0.00, holder keys absent | `API:277-285` status, `"balance":0.00` in the JSON text, no `cpf/nome/email/telefone` keys; `API:286-292` row nulls, balance 0, version 0; `CT:131-139`; `WalletResponseTest.java:32-36` | ✅ PASS |
| WHOLD-14 | userId missing/blank: 400 "User ID is required" | `API:316-318` `"Validation Failed"` and `"User ID is required"` for `""`, blank, `{}` and `null` (`API:323-326`); `CT:218,229` | ✅ PASS |
| WHOLD-15 | after migration existing wallets keep id/user_id/balance/version, 4 new columns null | `REPO:71-72` Flyway V1..V3 applied and the 4 columns nullable; `REPO:174-179` a row shaped like a legacy row is read with id, user, balance `150.50`, version `7` and an empty holder | ⚠️ Spec-precision gap: the test inserts a legacy-shaped row after V3 is applied; it never migrates data that existed at V2. The assertion matches the spec outcome (values and nulls) but not the trigger ("WHEN the migration is applied"). Low risk: `ADD COLUMN` without default/NOT NULL cannot alter existing rows. Stays Implementing |
| WHOLD-16 | response CPF masked `***.456.789-**` | `API:122` `assertEquals(masked(digits), response.jsonPath().getString("cpf"))`; `API:126-127` body has neither digits nor formatted CPF; `CT:97`; `CPF:132` | ✅ PASS |
| WHOLD-17 | response carries normalized nome, email, telefone | `API:123-125`; `CT:98-100`; `WalletResponseTest.java:55-57` | ✅ PASS |
| WHOLD-18 | no full CPF, e-mail or phone in logs | `API:441-472` class-wide Logback appender, every logger except one is scanned (`API:455`) for every CPF (digits and formatted), e-mail (raw, trimmed, lowercased) and phone used, including stack traces; vacuity guards `API:446-448`; `GEH:127` log is exactly `"CPF uniqueness violation"` at WARN, `GEH:114` no stack trace; `CreateWalletUseCaseTest.java:212-215`; `CT:267-271`; `CPF:141,150`; `WH:316-321`; `WalletTest.java:233-238`; `CreateWalletRequestTest.java:93-96` | ✅ PASS except the accepted gap. Stays Implementing |
| WHOLD-19 | no CPF value in any error message | 409 `API:197`, concurrent 409s `API:240`, 400 `API:347-349`, wrong type `API:399`; `GEH:108-109,206`; `CT:192,207,242`; messages fixed `CPF:126`, `WH:301-304` | ✅ PASS |
| WHOLD-20 | deposit and withdraw responses carry no holder fields | `API:429-432` after a real deposit and withdraw on a wallet with a full holder (`containsKey` false for the 4 keys, balances 100.00 and 70.00 at `API:425-426`); `CT:302-305,319-322`; `WalletResponseTest.java:32-36` | ✅ PASS |

### Edge cases from the spec

| Edge case | Evidence | Result |
| --------- | -------- | ------ |
| `cpf` as a JSON number: 400, no wallet | `API:396-401` 400, `error == "Bad Request"`, `message == "Malformed request body"`, no CPF in body, 0 rows; `CT:239-244`; `StrictStringDeserializerTest.java:46-70`; `CreateWalletRequestTest.java:82` (parameterized over all 4 fields); `GEH:204` | ✅ PASS |
| existing `userId` does not block a new CPF | `API:260-264` two 201s, `countByUserId == 2` | ✅ PASS |
| explicit `null` is absent | `API:170-179` 201, 4 keys absent, 4 columns null; `CT:149-152`; `StrictStringDeserializerTest.java:40` | ✅ PASS |
| failed write leaves no partial wallet (single transaction) | no test. By construction: one `INSERT` inside `@Transactional` `CreateWalletUseCase.execute` | ⚠️ Spec-precision gap: not asserted anywhere. Low value to test (one statement), but evidence-or-zero counts it as not covered |

**Status**: ⚠️ Spec-precision gaps flagged (WHOLD-15 trigger, transaction edge case). No assertion contradicts a spec-defined outcome. No vague assertion was found where the spec defines an exact value: status codes, message texts, the masked format and all boundaries (254/255, 150/151, 2/1, 8/7, 15/16) are asserted exactly.

Test hygiene: every new test carries a WHOLD id or an edge-case label in its `@DisplayName`. The only tests that map to no WHOLD id are the three "Regression" tests at `GEH:132-168` (existing Pix key, idempotency and generic integrity messages stay unchanged), which guard a handler this feature edited, so they are justified. Shallow assertions: none found; the log tests assert the appender is non-empty first, so they cannot pass vacuously.

---

## Discrimination Sensor

Run in a scratch worktree only, using narrowest covering classes (`-Dtest=...`; `-DexcludedGroups=docker` for non-docker). The expanded tier was applied (data integrity and PII).

| # | File:line | Mutation | Tests run | Killed? |
| - | --------- | -------- | --------- | ------- |
| M1 | `Cpf.java:35` | removed `!hasValidCheckDigits(digits)` | CpfTest, WalletHolderTest | ✅ Killed (5 failures, e.g. `CpfTest:52`, `:65`) |
| M2 | `Cpf.java:53` | mask shows digits 3-5 instead of 4-6 | CpfTest, WalletControllerTest, WalletResponseTest | ✅ Killed (4 failures, `CpfTest:132`, `CT:97`) |
| M3 | `WalletHolder.java:60` | stopped lowercasing e-mail | WalletHolderTest, WalletControllerTest | ✅ Killed (`WH:134`, `CT:99`) |
| M4 | `WalletHolder.java:20` | e-mail limit 254 -> 255 | WalletHolderTest | ✅ Killed (`WH:168`) |
| M5 | `WalletHolder.java:19` | phone `+` made optional | WalletHolderTest | ✅ Killed (`WH:202`, `WH:296`) |
| M6 | `CreateWalletUseCase.java:31` | pre-check inverted (`!existsByCpf`) | CreateWalletUseCaseTest | ✅ Killed (4 failures) |
| M7 | `GlobalExceptionHandler.java:300` | `uk_wallets_cpf` branch disabled (generic path runs) | GlobalExceptionHandlerTest | ✅ Killed (`GEH:107,127`) |
| M8 | `GlobalExceptionHandler.java:302` | race-branch log now prints `ex.getMessage()` | GlobalExceptionHandlerTest | ✅ Killed (`GEH:113->223`, `:127`) |
| M9 | `WalletController.java:52` | response returns unmasked CPF digits | WalletControllerTest | ✅ Killed (`CT:97`) |
| M10 | `WalletResponse.java:19` | removed `NON_NULL` on `cpf` | WalletResponseTest, WalletControllerTest | ✅ Killed (`WalletResponseTest:32`, `:90`) |
| M11 | `StrictStringDeserializer.java:25` | accepts any non-null token (numbers) | StrictStringDeserializerTest, CreateWalletRequestTest, WalletControllerTest | ✅ Killed (10 failures) |
| M12 | `Cpf.java:35` | removed all-equal-digits rejection | CpfTest | ✅ Killed (11 failures, `CPF:108`) |
| M13 | `WalletHolder.java:21` | name minimum 2 -> 1 | WalletHolderTest | ✅ Killed (`WH:232`, `:244`) |
| M15 | `V3__add_wallet_holder_data.sql:13` | unique index made non-partial (`WHERE cpf IS NOT NULL` removed) | WalletRepositoryIntegrationTest | ❌ Survived. Equivalent mutant: Postgres unique indexes treat NULLs as distinct, so the observable behavior (several wallets without CPF, WHOLD-12) is identical. Not a test gap and no fix task |
| M16 | `V3__add_wallet_holder_data.sql:9` | check constraint replaced by `CHECK (true)` | WalletRepositoryIntegrationTest | ✅ Killed (`REPO:159`) |
| M17 | `WalletRepositoryImpl.java:50` | `existsByCpf` negated | WalletRepositoryIntegrationTest | ✅ Killed (`REPO:121`) |
| M18 | `WalletRepositoryImpl.java:81` | `toDomain` drops `telefone` | WalletRepositoryIntegrationTest | ✅ Killed (`REPO:82`) |
| M19 | `WalletRepositoryImpl.java:63` | `toJpaEntity` drops `nome` | WalletRepositoryIntegrationTest | ✅ Killed (`REPO:82`) |
| M20 | `WalletController.java:94` | deposit response gets `nome` | WalletControllerTest | ✅ Killed (`CT:303`) |
| M21 | `WalletHolder.java:57` | e-mail length check removed (regex only) | WalletHolderTest | ✅ Killed (`WH:168`) |
| M22 | `CreateWalletUseCase.java:31` | pre-check removed entirely | CreateWalletUseCaseTest | ✅ Killed (4 failures) |

Two attempted mutations were discarded as invalid, not counted: a first M10 that only inserted a comment, and an M14 (drop `@JsonDeserialize` on one field) whose sed did not apply. Per-field strictness is anyway covered by the parameterized `CreateWalletRequestTest.java:82` over all four fields.

**Sensor depth**: expanded (data integrity + PII), 21 valid mutations
**Result**: 20/21 killed, 1 survivor is an equivalent mutant (M15). No fix task. PASS ✅

Isolation: the scratch worktree was removed with `git worktree remove --force`; `git status --porcelain` of the real tree after the sensor is byte-identical to the baseline taken before any work (checked with `diff`).

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ Two value objects, two exceptions, one deserializer; nothing speculative. `WalletRepositoryImpl.toJpaEntity` has a null guard on `wallet.getHolder()` that the builder default makes unreachable; harmless defensive line |
| Surgical changes | ✅ `git diff 698ff49..HEAD --name-status` touches only files listed in `tasks.md` plus `pom.xml` (T19) and the README hunk (T18). No pre-existing test modified: `--numstat -- src/test` shows 0 deleted lines for every file |
| No scope creep | ✅ One side effect beyond the spec, documented in design Risks: `HttpMessageNotReadableException` now returns 400 for every endpoint instead of 500 |
| Matches patterns | ✅ `Cpf.of` follows `Money.of`; handlers follow `ErrorResponse.builder()`; e-mail regex copied from `PixKey` |
| AD-001 respected | ✅ No Bean Validation on PII (`CreateWalletRequest` keeps only `@NotBlank` on `userId`); the two PII-carrying exceptions are logged with fixed text (`GlobalExceptionHandler.java:302`, and the unreadable-body handler); `ex.getMessage()` appears only in the pre-existing handlers and in `handleDadosTitularInvalidos`, whose message is fixed. `@ToString.Exclude` on holder fields in `Wallet`, `WalletJpaEntity`, `CreateWalletRequest`, `WalletHolder`; `Cpf.toString()` masked |
| No stray debug code | ✅ none found |
| Spec-anchored outcome check | ✅ asserted values match the spec outcome, 2 precision gaps flagged above |
| Per-layer Coverage Expectation | ✅ domain 1:1 with ACs (`CpfTest`, `WalletHolderTest`); controller happy/edge/error (`WalletControllerTest`); handler branches incl. log content (`GlobalExceptionHandlerTest`); repository constraints (`WalletRepositoryIntegrationTest`); full HTTP path (`WalletHolderApiTest`) |
| Every test maps to a requirement | ✅ see test hygiene note above |
| Documented guidelines | none - strong defaults applied (no project testing guideline exists) |
| Accepted-gap containment | ✅ `WalletHolderApiTest.java:455` skips exactly one logger name, `org.hibernate.engine.jdbc.spi.SqlExceptionHelper`, via `HIBERNATE_SQL_EXCEPTION_LOGGER` (`:65`); all other loggers, messages and stack traces are scanned. Documented in spec Assumptions (row "Log do Hibernate...", `spec.md:53`), WHOLD-18 text (`spec.md:121`), last Success Criterion (`spec.md:174`) and design Risks (`design.md:194`) |
| Other PII exposure paths | ✅ none found. `org.hibernate.SQL: DEBUG` (`application.yml:63`) logs statements with `?` placeholders; parameter binding loggers are not enabled. Unreachable-by-API note: a `ck_wallets_cpf_format` violation would fall into the generic integrity branch and log `ex.getMessage()` (Postgres "Failing row contains ..."), but domain validation makes it unreachable through `POST /wallets` |

---

## Edge Cases

- [x] CPF as JSON number gives 400 "Malformed request body"
- [x] Existing `userId` does not block a new CPF
- [x] Explicit `null` is treated as absent
- [ ] Failed write leaves no partial wallet: satisfied by construction, no test (⚠️ spec-precision gap, non-blocking)

---

## Gate Check

- **Gate command**: `"$MVN" -B -o verify` (Full build gate, Docker running), per `tasks.md` Gate Check Commands
- **Clean checkout of HEAD (`4497deb`)**: ❌ BUILD FAILURE at `compile`. 4 errors "method does not override or implement a method from a supertype": `PixTransferRepositoryImpl.java:27`, `LedgerEntryRepositoryImpl.java:29`, `PixKeyRepositoryImpl.java:34`, `IdempotencyRepositoryImpl.java:28`. These `@Override` annotations are committed code that predates the feature (the four files are unchanged between `698ff49` and `HEAD`), so the pre-feature commit fails the same way. The user's uncommitted working-tree edits (removing those four `@Override` lines, and one `verify(...never()).findByEndToEndId` line in `ProcessWebhookUseCaseTest.java:249`) are what make the project compile. The feature's own baseline of 111 tests was therefore measured on the dirty tree. CI (`ubuntu-latest`, `./mvnw`) will fail on this branch until those edits are committed.
- **Clean HEAD + only those 5 pre-existing local edits applied (scratch worktree)**: ✅ BUILD SUCCESS
- **Result**: 325 run, 321 passed, 0 failed, 0 errors, 4 skipped
- **Test count before feature**: 111 (107 pass, 4 skipped)
- **Test count after feature**: 325 (321 pass, 4 skipped)
- **Delta**: +214 new tests, 0 removed (`--numstat -- src/test`: 0 deleted lines in all 11 changed test files)
- **Skipped tests**: the same 4 pre-existing `@Disabled` classes, each a single skipped test: `PixControllerRestAssuredTest`, `WalletControllerIntegrationTest`, `WalletControllerRestAssuredTest`, `PixTransferEndToEndTest`. No new `@Disabled` (`git diff 698ff49..HEAD -- src/test | grep -c @Disabled` = 0). Justification: disabled before the feature (design Risks row 5); `WalletHolderApiTest` covers the HTTP layer instead
- **Docker classes in the run**: `WalletHolderApiTest` 41 tests and `WalletRepositoryIntegrationTest` 8 tests, 0 failures
- **Failures**: none (after the pre-existing compile blocker is neutralised)

---

## Fix Plans (if issues found)

### Fix 1: Clean HEAD does not compile (pre-existing, outside the feature)

- **Root cause**: committed `@Override` on `findByWalletId`, `findByEndToEndId` and similar methods that the port interfaces do not declare; the user already fixed it locally and has not committed it.
- **Fix task**: commit the 4 `@Override` removals and the `ProcessWebhookUseCaseTest` line as their own commit (`fix(build): ...`), outside this feature's commits.
- **Priority**: Major for CI, not a defect of this feature.

### Fix 2 (optional, Minor): add a rollback or migration-over-data test

- WHOLD-15: a test that migrates to V2, inserts a row, then migrates to V3 and reads it back.
- Edge case "no partial wallet": a use case test where `save` throws and nothing is left behind (only meaningful with an integration test).

---

## Accepted deviations

| Deviation | Where documented | Containment verified | Effect on status |
| --------- | ---------------- | -------------------- | ---------------- |
| On the concurrent duplicate-CPF path, Hibernate logs the Postgres `Detail: Key (cpf)=(...)` in ERROR through logger `org.hibernate.engine.jdbc.spi.SqlExceptionHelper`, before the exception reaches `GlobalExceptionHandler`. Accepted by the user on 2026-11-01. Recommended follow-up (in spec): Logback masking filter or logger OFF. | `spec.md:53`, `spec.md:121`, `spec.md:174`, `design.md:194` | `WalletHolderApiTest.java:455` excludes only that one logger; every other logger, message and stack trace is still scanned for CPF, e-mail and phone | WHOLD-11 and WHOLD-18 stay "Implementing" (accepted gap), not "Verified" |

---

## Requirement Traceability Update

Proposal only; `spec.md` was not edited by the Verifier.

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| WHOLD-01 | Verified | ✅ Verified |
| WHOLD-02 | Verified | ✅ Verified |
| WHOLD-03 | Verified | ✅ Verified |
| WHOLD-04 | Verified | ✅ Verified |
| WHOLD-05 | Verified | ✅ Verified |
| WHOLD-06 | Verified | ✅ Verified |
| WHOLD-07 | Verified | ✅ Verified |
| WHOLD-08 | Verified | ✅ Verified |
| WHOLD-09 | Verified | ✅ Verified |
| WHOLD-10 | Verified | ✅ Verified |
| WHOLD-11 | Implementing | Implementing (accepted gap: Hibernate log) |
| WHOLD-12 | Verified | ✅ Verified |
| WHOLD-13 | Verified | ✅ Verified |
| WHOLD-14 | Verified | ✅ Verified |
| WHOLD-15 | Implementing | Implementing (spec-precision gap: no migrate-over-existing-data test) |
| WHOLD-16 | Verified | ✅ Verified |
| WHOLD-17 | Verified | ✅ Verified |
| WHOLD-18 | Implementing | Implementing (accepted gap: Hibernate log) |
| WHOLD-19 | Verified | ✅ Verified |
| WHOLD-20 | Verified | ✅ Verified |

---

## Summary

**Verdict**: PASS

**Overall**: ✅ Ready for the feature itself. ⚠️ The branch needs the pre-existing build fix committed before CI can go green.

**Spec-anchored check**: 18/20 ACs matched the spec outcome with exact assertions; 2 ACs (WHOLD-11, WHOLD-18) match but carry the accepted logging gap; 2 spec-precision gaps flagged (WHOLD-15 trigger, "no partial wallet" edge case).
**Sensor**: 20/21 mutations killed; the 1 survivor (non-partial unique index) is a behavior-equivalent mutant.
**Gate**: 325 run, 321 passed, 0 failed, 4 skipped (same 4 pre-existing `@Disabled`), measured on clean HEAD plus the 5 pre-existing local edits; clean HEAD alone fails to compile for reasons unrelated to the feature.

**What works**: optional holder data on `POST /wallets` with exact validation messages and boundaries; CPF normalization, masking and uniqueness (pre-check plus partial unique index), including 20-way concurrency giving one 201 and nineteen 409; legacy userId-only clients unchanged; deposit and withdraw responses free of holder data; PII kept out of error bodies and logs except the accepted Hibernate logger.

**Issues found**:
1. Clean HEAD does not compile (4 `@Override` errors in files the feature did not touch). Commit the user's local fix separately.
2. WHOLD-15 test is a legacy-shaped row, not a real migrate-over-data run (Minor).
3. "No partial wallet" edge case has no test (Minor).

**Next steps**: commit the build fix (Fix 1); optionally add the two tests in Fix 2; update `spec.md` statuses per the table above; keep the Hibernate log follow-up (Logback masking filter) on the backlog.
