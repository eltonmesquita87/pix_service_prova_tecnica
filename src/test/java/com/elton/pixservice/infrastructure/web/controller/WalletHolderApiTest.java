package com.elton.pixservice.infrastructure.web.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.elton.pixservice.infrastructure.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end tests of {@code POST /wallets} with holder data, over HTTP against a real Postgres.
 *
 * <p>The server runs on its own thread and commits, so rows survive between methods: every method draws a
 * fresh valid CPF and a unique userId. A Logback appender on the root logger records everything the
 * application logs during the class; the last method proves no CPF, e-mail or phone reached the logs.
 */
@Tag("docker")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("POST /wallets holder data end to end")
class WalletHolderApiTest extends BaseIntegrationTest {

    private static final String PHONE = "+5511999999999";
    private static final String HIBERNATE_SQL_EXCEPTION_LOGGER = "org.hibernate.engine.jdbc.spi.SqlExceptionHelper";

    /** Every CPF (digits and formatted), e-mail and phone the class sends; none may reach a log. */
    private static final Set<String> SENSITIVE = ConcurrentHashMap.newKeySet();

    private static ListAppender<ILoggingEvent> appender;
    private static Logger rootLogger;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void attachLogAppender() {
        rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        appender = new ListAppender<>();
        appender.setContext(rootLogger.getLoggerContext());
        appender.start();
        rootLogger.addAppender(appender);
    }

    /**
     * Spring Boot resets the Logback context when it loads the application context, which happens after
     * {@code @BeforeAll} and drops any appender attached earlier. Attach it again before every method
     * (a no-op once it is attached); the requests of all methods are made after this point.
     */
    @BeforeEach
    void ensureLogAppenderAttached() {
        if (!rootLogger.isAttached(appender)) {
            appender.setContext(rootLogger.getLoggerContext());
            appender.start();
            rootLogger.addAppender(appender);
        }
    }

    @AfterAll
    static void detachLogAppender() {
        rootLogger.detachAppender(appender);
        appender.stop();
    }

    // ---------------------------------------------------------------- creation with holder data

    @Test
    @Order(1)
    @DisplayName("WHOLD-01, WHOLD-03, WHOLD-05, WHOLD-08, WHOLD-16, WHOLD-17: full body gives 201, masked CPF and normalized row")
    void fullBodyIsPersistedNormalizedAndCpfIsMasked() {
        String digits = newCpf();
        String email = "  Maria.SILVA@Example.COM  ";
        trackEmail(email);
        SENSITIVE.add(PHONE);
        String userId = newUserId();

        Response response = createWallet(body(userId, format(digits), "  Maria Silva  ", email, PHONE));

        assertEquals(201, response.statusCode());
        assertEquals(userId, response.jsonPath().getString("userId"));
        assertNotNull(response.jsonPath().getLong("id"));
        assertEquals(masked(digits), response.jsonPath().getString("cpf"));
        assertEquals("Maria Silva", response.jsonPath().getString("nome"));
        assertEquals("maria.silva@example.com", response.jsonPath().getString("email"));
        assertEquals(PHONE, response.jsonPath().getString("telefone"));
        assertFalse(response.asString().contains(digits), "response must not carry the full CPF digits");
        assertFalse(response.asString().contains(format(digits)), "response must not carry the formatted CPF");

        Map<String, Object> row = rowById(response.jsonPath().getLong("id"));
        assertEquals(digits, row.get("cpf"));
        assertEquals("Maria Silva", row.get("nome"));
        assertEquals("maria.silva@example.com", row.get("email"));
        assertEquals(PHONE, row.get("telefone"));
        assertEquals(userId, row.get("user_id"));
    }

    @Test
    @Order(2)
    @DisplayName("WHOLD-01: any subset of valid fields is accepted and the omitted ones stay absent")
    void subsetOfFieldsIsAccepted() {
        String email = "subset." + UUID.randomUUID() + "@example.com";
        trackEmail(email);
        String userId = newUserId();

        Response response = createWallet(body(userId, null, "Joao Souza", email, null));

        assertEquals(201, response.statusCode());
        assertEquals("Joao Souza", response.jsonPath().getString("nome"));
        assertEquals(email, response.jsonPath().getString("email"));
        Map<String, Object> json = response.jsonPath().getMap("$");
        assertFalse(json.containsKey("cpf"), "cpf was not sent, so it must not be in the response");
        assertFalse(json.containsKey("telefone"), "telefone was not sent, so it must not be in the response");

        Map<String, Object> row = rowById(response.jsonPath().getLong("id"));
        assertNull(row.get("cpf"));
        assertNull(row.get("telefone"));
        assertEquals("Joao Souza", row.get("nome"));
        assertEquals(email, row.get("email"));
    }

    @Test
    @Order(3)
    @DisplayName("Edge case: explicit null in a holder field is treated as absent")
    void explicitNullIsTreatedAsAbsent() {
        String userId = newUserId();
        String json = "{\"userId\":\"" + userId + "\",\"cpf\":null,\"nome\":null,\"email\":null,\"telefone\":null}";

        Response response = createWallet(json);

        assertEquals(201, response.statusCode());
        Map<String, Object> keys = response.jsonPath().getMap("$");
        for (String key : new String[] {"cpf", "nome", "email", "telefone"}) {
            assertFalse(keys.containsKey(key), key + " must be absent");
        }
        Map<String, Object> row = rowById(response.jsonPath().getLong("id"));
        assertNull(row.get("cpf"));
        assertNull(row.get("nome"));
        assertNull(row.get("email"));
        assertNull(row.get("telefone"));
    }

    // ---------------------------------------------------------------- CPF uniqueness

    @Test
    @Order(4)
    @DisplayName("WHOLD-03, WHOLD-10, WHOLD-19: same CPF formatted then digits-only gives 201 then 409 and one row")
    void duplicateCpfIsRejectedRegardlessOfMask() {
        String digits = newCpf();

        Response first = createWallet(body(newUserId(), format(digits), null, null, null));
        Response second = createWallet(body(newUserId(), digits, null, null, null));

        assertEquals(201, first.statusCode());
        assertEquals(409, second.statusCode());
        assertEquals("CPF already registered", second.jsonPath().getString("message"));
        assertEquals("Conflict", second.jsonPath().getString("error"));
        assertNoCpfIn(second.asString(), digits);
        assertEquals(1, countByCpf(digits));
    }

    @Test
    @Order(5)
    @DisplayName("WHOLD-11, WHOLD-18, WHOLD-19: 20 concurrent requests with the same CPF create exactly one wallet")
    void concurrentRequestsWithSameCpfCreateOneWallet() throws Exception {
        final int requests = 20;
        String digits = newCpf();
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Response>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < requests; i++) {
                String payload = body(newUserId(), i % 2 == 0 ? digits : format(digits), null, null, null);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return RestAssured.given().port(port).contentType(ContentType.JSON).body(payload)
                            .when().post("/wallets").then().extract().response();
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "the 20 workers did not start in time");
            go.countDown();

            List<Response> responses = new ArrayList<>();
            for (Future<Response> future : futures) {
                try {
                    responses.add(future.get(30, TimeUnit.SECONDS));
                } catch (TimeoutException e) {
                    fail("Not all 20 concurrent requests completed within 30 seconds");
                }
            }

            long created = responses.stream().filter(r -> r.statusCode() == 201).count();
            long conflicts = responses.stream().filter(r -> r.statusCode() == 409).count();
            assertEquals(1, created, "exactly one request must create the wallet");
            assertEquals(requests - 1, conflicts, "every other request must get 409");
            for (Response response : responses) {
                if (response.statusCode() == 409) {
                    assertEquals("CPF already registered", response.jsonPath().getString("message"));
                    assertNoCpfIn(response.asString(), digits);
                }
            }
            assertEquals(1, countByCpf(digits));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Order(6)
    @DisplayName("Edge case: an existing userId does not block a new wallet with a different CPF")
    void existingUserIdDoesNotBlockNewCpf() {
        String userId = newUserId();
        String firstCpf = newCpf();
        String secondCpf = newCpf();

        Response first = createWallet(body(userId, firstCpf, null, null, null));
        Response second = createWallet(body(userId, secondCpf, null, null, null));

        assertEquals(201, first.statusCode());
        assertEquals(201, second.statusCode());
        assertEquals(1, countByCpf(firstCpf));
        assertEquals(1, countByCpf(secondCpf));
        assertEquals(2, countByUserId(userId));
    }

    // ---------------------------------------------------------------- compatibility with userId-only clients

    @Test
    @Order(7)
    @DisplayName("WHOLD-13, WHOLD-15: only userId gives 201, balance 0.00 and no holder keys; holder columns stay null")
    void userIdOnlyKeepsLegacyBehavior() {
        String userId = newUserId();

        Response response = createWallet("{\"userId\":\"" + userId + "\"}");

        assertEquals(201, response.statusCode());
        assertEquals(userId, response.jsonPath().getString("userId"));
        assertNotNull(response.jsonPath().getLong("id"));
        assertEquals(0, new BigDecimal(response.jsonPath().getString("balance")).compareTo(BigDecimal.ZERO));
        assertTrue(response.asString().contains("\"balance\":0.00"), "balance must serialize as 0.00: " + response.asString());
        Map<String, Object> json = response.jsonPath().getMap("$");
        for (String key : new String[] {"cpf", "nome", "email", "telefone"}) {
            assertFalse(json.containsKey(key), key + " must be absent");
        }
        Map<String, Object> row = rowById(response.jsonPath().getLong("id"));
        assertNull(row.get("cpf"));
        assertNull(row.get("nome"));
        assertNull(row.get("email"));
        assertNull(row.get("telefone"));
        assertEquals(0, ((BigDecimal) row.get("balance")).compareTo(BigDecimal.ZERO));
        assertEquals(0L, ((Number) row.get("version")).longValue());
    }

    @Test
    @Order(8)
    @DisplayName("WHOLD-12: several wallets without CPF are all accepted")
    void walletsWithoutCpfAreNotDuplicates() {
        Response first = createWallet("{\"userId\":\"" + newUserId() + "\"}");
        Response second = createWallet("{\"userId\":\"" + newUserId() + "\"}");

        assertEquals(201, first.statusCode());
        assertEquals(201, second.statusCode());
        assertTrue(first.jsonPath().getLong("id") != second.jsonPath().getLong("id"));
        assertNull(rowById(first.jsonPath().getLong("id")).get("cpf"));
        assertNull(rowById(second.jsonPath().getLong("id")).get("cpf"));
    }

    @ParameterizedTest(name = "[{index}] userId payload {0}")
    @MethodSource("invalidUserIdPayloads")
    @Order(9)
    @DisplayName("WHOLD-14: blank or missing userId gives 400 'User ID is required'")
    void blankOrMissingUserIdIsRejected(String label, String payload) {
        Response response = createWallet(payload);

        assertEquals(400, response.statusCode());
        assertEquals("Validation Failed", response.jsonPath().getString("error"));
        assertEquals("User ID is required", response.jsonPath().getString("message"));
    }

    static Stream<Arguments> invalidUserIdPayloads() {
        return Stream.of(
                Arguments.of("empty", "{\"userId\":\"\"}"),
                Arguments.of("blank", "{\"userId\":\"   \"}"),
                Arguments.of("missing", "{}"),
                Arguments.of("null", "{\"userId\":null}"));
    }

    // ---------------------------------------------------------------- validation

    @ParameterizedTest(name = "[{index}] {0}: {1}")
    @MethodSource("invalidHolderFields")
    @Order(10)
    @DisplayName("WHOLD-02, WHOLD-04, WHOLD-06, WHOLD-07, WHOLD-09, WHOLD-19: invalid holder field gives 400 and no row")
    void invalidHolderFieldIsRejectedAndNothingIsPersisted(String field, String label, String value, String message) {
        String userId = newUserId();
        track(field, value);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put(field, value);

        Response response = createWallet(json(payload));

        assertEquals(400, response.statusCode());
        assertEquals("Bad Request", response.jsonPath().getString("error"));
        assertEquals(message, response.jsonPath().getString("message"));
        if ("cpf".equals(field) && !value.trim().isEmpty()) {
            assertFalse(response.asString().contains(value), "400 body must not echo the rejected CPF");
        }
        assertEquals(0, countByUserId(userId), "an invalid holder field must not create a wallet");
    }

    static Stream<Arguments> invalidHolderFields() {
        String validDigits = newCpf();
        String wrongCheckDigit = validDigits.substring(0, 10) + (validDigits.charAt(10) == '0' ? '1' : '0');
        String longEmail = repeat("a", 244) + "@example.com";   // 256 chars
        String boundaryEmail = repeat("a", 243) + "@example.com"; // 255 chars
        return Stream.of(
                Arguments.of("cpf", "wrong check digit", wrongCheckDigit, "Invalid CPF"),
                Arguments.of("cpf", "10 digits", validDigits.substring(0, 10), "Invalid CPF"),
                Arguments.of("cpf", "12 digits", validDigits + "1", "Invalid CPF"),
                Arguments.of("cpf", "letters", "abcdefghijk", "Invalid CPF"),
                Arguments.of("cpf", "spaces as separators", "123 456 789 09", "Invalid CPF"),
                Arguments.of("cpf", "all equal digits", "00000000000", "Invalid CPF"),
                Arguments.of("cpf", "empty", "", "Invalid CPF"),
                Arguments.of("cpf", "blank", "   ", "Invalid CPF"),
                Arguments.of("email", "no at sign", "not-an-email", "Invalid email"),
                Arguments.of("email", "no top level domain", "a@b", "Invalid email"),
                Arguments.of("email", "256 chars", longEmail, "Invalid email"),
                Arguments.of("email", "255 chars", boundaryEmail, "Invalid email"),
                Arguments.of("email", "empty", "", "Invalid email"),
                Arguments.of("email", "blank", "   ", "Invalid email"),
                Arguments.of("telefone", "no plus", "5511999999999", "Invalid phone"),
                Arguments.of("telefone", "leading zero", "+0123456789", "Invalid phone"),
                Arguments.of("telefone", "7 digits", "+1234567", "Invalid phone"),
                Arguments.of("telefone", "16 digits", "+1234567890123456", "Invalid phone"),
                Arguments.of("telefone", "letters", "+55abc", "Invalid phone"),
                Arguments.of("telefone", "empty", "", "Invalid phone"),
                Arguments.of("telefone", "blank", "   ", "Invalid phone"),
                Arguments.of("nome", "1 char", "A", "Invalid name"),
                Arguments.of("nome", "1 char padded", "  A  ", "Invalid name"),
                Arguments.of("nome", "151 chars", repeat("n", 151), "Invalid name"),
                Arguments.of("nome", "empty", "", "Invalid name"),
                Arguments.of("nome", "blank", "   ", "Invalid name"));
    }

    @Test
    @Order(11)
    @DisplayName("Edge case: cpf sent as a JSON number gives 400 'Malformed request body' and no row")
    void cpfAsJsonNumberIsRejected() {
        String digits = newCpf();
        String userId = newUserId();

        Response response = createWallet("{\"userId\":\"" + userId + "\",\"cpf\":" + digits + "}");

        assertEquals(400, response.statusCode());
        assertEquals("Bad Request", response.jsonPath().getString("error"));
        assertEquals("Malformed request body", response.jsonPath().getString("message"));
        assertNoCpfIn(response.asString(), digits);
        assertEquals(0, countByUserId(userId));
        assertEquals(0, countByCpf(digits));
    }

    // ---------------------------------------------------------------- deposit and withdraw

    @Test
    @Order(12)
    @DisplayName("WHOLD-20: deposit and withdraw responses carry none of the holder fields")
    void depositAndWithdrawResponsesCarryNoHolderFields() {
        String digits = newCpf();
        String email = "ops." + UUID.randomUUID() + "@example.com";
        trackEmail(email);
        SENSITIVE.add(PHONE);
        Response created = createWallet(body(newUserId(), digits, "Ana Lima", email, PHONE));
        assertEquals(201, created.statusCode());
        long id = created.jsonPath().getLong("id");

        Response deposit = given().body("{\"amount\":100.00}").when().post("/wallets/{id}/deposit", id)
                .then().extract().response();
        Response withdraw = given().body("{\"amount\":30.00}").when().post("/wallets/{id}/withdraw", id)
                .then().extract().response();

        assertEquals(200, deposit.statusCode());
        assertEquals(200, withdraw.statusCode());
        assertEquals(0, new BigDecimal("100.00").compareTo(new BigDecimal(deposit.jsonPath().getString("balance"))));
        assertEquals(0, new BigDecimal("70.00").compareTo(new BigDecimal(withdraw.jsonPath().getString("balance"))));
        for (Response response : new Response[] {deposit, withdraw}) {
            Map<String, Object> json = response.jsonPath().getMap("$");
            for (String key : new String[] {"cpf", "nome", "email", "telefone"}) {
                assertFalse(json.containsKey(key), key + " must be absent from " + response.asString());
            }
            assertNoCpfIn(response.asString(), digits);
        }
    }

    // ---------------------------------------------------------------- log privacy (must run last)

    @Test
    @Order(100)
    @DisplayName("WHOLD-18, WHOLD-19: no log event of the whole class holds a CPF, e-mail or phone used")
    void logsNeverContainCpfEmailOrPhone() {
        List<ILoggingEvent> events;
        synchronized (appender) {
            events = new ArrayList<>(appender.list);
        }
        assertFalse(events.isEmpty(), "the appender captured nothing, so this check would be vacuous");
        assertTrue(events.stream().anyMatch(e -> e.getFormattedMessage().contains("Received request to create wallet")),
                "the appender did not capture the application's own request logs");
        assertFalse(SENSITIVE.isEmpty());

        List<String> leaks = new ArrayList<>();
        for (ILoggingEvent event : events) {
            // KNOWN GAP accepted by the user on 2026-11-01 (see spec Assumptions and design Risks): on the concurrent
            // duplicate CPF path Hibernate logs the Postgres "Key (cpf)=(...)" detail. Only this logger is skipped.
            if (HIBERNATE_SQL_EXCEPTION_LOGGER.equals(event.getLoggerName())) {
                continue;
            }
            StringBuilder text = new StringBuilder(event.getFormattedMessage());
            if (event.getThrowableProxy() != null) {
                text.append('\n').append(event.getThrowableProxy().getMessage())
                        .append('\n').append(ThrowableProxyUtil.asString(event.getThrowableProxy()));
            }
            String all = text.toString();
            for (String secret : SENSITIVE) {
                if (all.contains(secret)) {
                    leaks.add("logger=" + event.getLoggerName() + " level=" + event.getLevel()
                            + " leaked a value of length " + secret.length());
                }
            }
        }
        assertTrue(leaks.isEmpty(), "sensitive values reached the logs: " + leaks);
    }

    // ---------------------------------------------------------------- helpers

    private Response createWallet(String jsonBody) {
        return given().body(jsonBody).when().post("/wallets").then().extract().response();
    }

    private Map<String, Object> rowById(long id) {
        return jdbc.queryForMap("SELECT user_id, cpf, nome, email, telefone, balance, version FROM wallets WHERE id = ?", id);
    }

    private int countByCpf(String digits) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM wallets WHERE cpf = ?", Integer.class, digits);
    }

    private int countByUserId(String userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM wallets WHERE user_id = ?", Integer.class, userId);
    }

    private static void assertNoCpfIn(String text, String digits) {
        assertFalse(text.contains(digits), "body must not contain the CPF digits: " + text);
        assertFalse(text.contains(format(digits)), "body must not contain the formatted CPF: " + text);
    }

    private static String newUserId() {
        return "holder-" + UUID.randomUUID();
    }

    /** Registers a rejected contact value; short values are skipped because they would match unrelated log text. */
    private static void track(String field, String value) {
        if (!"nome".equals(field) && value.trim().length() >= 8) {
            SENSITIVE.add(value.trim());
        }
    }

    private static void trackEmail(String email) {
        SENSITIVE.add(email);
        SENSITIVE.add(email.trim());
        SENSITIVE.add(email.trim().toLowerCase());
    }

    private static String repeat(String s, int times) {
        return String.join("", Collections.nCopies(times, s));
    }

    /** Builds a JSON body with only the non-null holder fields. */
    private static String body(String userId, String cpf, String nome, String email, String telefone) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        if (cpf != null) payload.put("cpf", cpf);
        if (nome != null) payload.put("nome", nome);
        if (email != null) payload.put("email", email);
        if (telefone != null) payload.put("telefone", telefone);
        return json(payload);
    }

    private static String json(Map<String, Object> payload) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) out.append(',');
            first = false;
            out.append('"').append(entry.getKey()).append("\":\"")
                    .append(String.valueOf(entry.getValue()).replace("\\", "\\\\").replace("\"", "\\\""))
                    .append('"');
        }
        return out.append('}').toString();
    }

    /** Draws a fresh valid CPF (correct check digits, not all digits equal), registers it as sensitive, returns the 11 digits. */
    private static String newCpf() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int[] d = new int[11];
        boolean allEqual;
        do {
            allEqual = true;
            for (int i = 0; i < 9; i++) {
                d[i] = random.nextInt(10);
                if (d[i] != d[0]) allEqual = false;
            }
        } while (allEqual);
        d[9] = checkDigit(d, 9);
        d[10] = checkDigit(d, 10);
        StringBuilder sb = new StringBuilder();
        for (int digit : d) sb.append(digit);
        String digits = sb.toString();
        SENSITIVE.add(digits);
        SENSITIVE.add(format(digits));
        return digits;
    }

    private static int checkDigit(int[] d, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += d[i] * (length + 1 - i);
        }
        return (sum * 10 % 11) % 10;
    }

    private static String format(String digits) {
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    private static String masked(String digits) {
        return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
    }
}
