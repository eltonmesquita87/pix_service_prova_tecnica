package com.elton.pixservice.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Simulação de performance completa para a API Pix Service.
 *
 * Testa os seguintes cenários:
 * - Criação de carteiras
 * - Registro de chaves Pix
 * - Depósitos em carteiras
 * - Transferências Pix com idempotência
 * - Processamento de webhooks
 * - Consulta de saldo
 * - Saques
 *
 * Configuração padrão:
 * - Ramp-up: 0 → 1000 usuários em 2 minutos
 * - Duração: 10 minutos
 * - Ramp-down: 1 minuto
 * - Pausa entre requisições: 100-500ms (aleatória)
 *
 * Massas de dados (arquivos CSV):
 * - wallets.csv: 50 userIds para criação de carteiras
 * - deposits.csv: 50 valores de depósito (R$ 500 a R$ 3.000)
 * - withdrawals.csv: 50 valores de saque (R$ 25 a R$ 150)
 * - pix-keys.csv: 50 chaves Pix (CPF, EMAIL, PHONE, EVP)
 * - transfers.csv: 50 valores de transferência (apenas amount)
 *
 * IDs Dinâmicos (gerados em tempo de execução):
 * - Idempotency-Key: UUID único para cada transferência
 * - endToEndId: Gerado pela API e reutilizado no webhook
 * - eventId: UUID único para cada evento de webhook
 * - eventType: 70% CONFIRMED, 30% REJECTED (baseado em hash)
 */
public class PixServiceSimulation extends Simulation {

    // ========== CONFIGURAÇÃO ==========

    private static final String BASE_URL = System.getProperty("base.url", "http://localhost:8080");
    private static final int USERS = Integer.parseInt(System.getProperty("users", "1000"));
    private static final int RAMPUP_DURATION = Integer.parseInt(System.getProperty("rampup.duration", "120"));
    private static final int TEST_DURATION = Integer.parseInt(System.getProperty("test.duration", "600"));
    private static final int RAMPDOWN_DURATION = Integer.parseInt(System.getProperty("rampdown.duration", "60"));

    // ========== HTTP PROTOCOL ==========

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    // ========== FEEDERS ==========

    private FeederBuilder<String> walletFeeder = csv("data/wallets.csv").circular();
    private FeederBuilder<String> depositFeeder = csv("data/deposits.csv").circular();
    private FeederBuilder<String> withdrawalFeeder = csv("data/withdrawals.csv").circular();
    private FeederBuilder<String> pixKeyFeeder = csv("data/pix-keys.csv").circular();
    private FeederBuilder<String> transferFeeder = csv("data/transfers.csv").circular();
    // Webhook não usa feeder - usa endToEndId dinâmico da transferência

    // ========== CENÁRIOS ==========

    /**
     * Cenário 1: Gestão de Carteiras
     */
    private ScenarioBuilder walletManagementScenario = scenario("Wallet Management")
            .feed(walletFeeder)
            .exec(
                    // 1. Criar carteira com userId da massa
                    http("Create Wallet")
                            .post("/wallets")
                            .body(StringBody(session ->
                                    "{\"userId\":\"" + session.getString("userId") + "\"}"
                            ))
                            .check(status().is(201))
                            .check(jsonPath("$.id").saveAs("walletId"))
                            .check(jsonPath("$.balance").exists())
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .exec(
                    // 2. Consultar saldo inicial
                    http("Get Initial Balance")
                            .get(session -> "/wallets/" + session.getLong("walletId") + "/balance")
                            .check(status().is(200))
                            .check(jsonPath("$.balance").exists())
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(depositFeeder)
            .exec(
                    // 3. Fazer depósito com valor da massa
                    http("Deposit")
                            .post(session -> "/wallets/" + session.getLong("walletId") + "/deposit")
                            .body(StringBody(session ->
                                    "{\"amount\":" + session.getString("amount") + "}"
                            ))
                            .check(status().is(200))
                            .check(jsonPath("$.balance").saveAs("currentBalance"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .exec(
                    // 4. Consultar saldo após depósito
                    http("Get Balance After Deposit")
                            .get(session -> "/wallets/" + session.getLong("walletId") + "/balance")
                            .check(status().is(200))
                            .check(jsonPath("$.balance").exists())
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(pixKeyFeeder)
            .exec(
                    // 5. Registrar chave Pix com dados da massa
                    http("Register Pix Key")
                            .post(session -> "/wallets/" + session.getLong("walletId") + "/pix-keys")
                            .body(StringBody(session ->
                                    "{"
                                    + "\"keyType\":\"" + session.getString("keyType") + "\","
                                    + "\"keyValue\":\"" + session.getString("keyValue") + "\""
                                    + "}"
                            ))
                            .check(status().in(201, 409))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500));

    /**
     * Cenário 2: Transferências Pix
     */
    private ScenarioBuilder pixTransferScenario = scenario("Pix Transfer")
            .feed(walletFeeder)
            .exec(
                    // 1. Criar carteira de origem com dados da massa
                    http("Create Source Wallet")
                            .post("/wallets")
                            .body(StringBody(session ->
                                    "{\"userId\":\"" + session.getString("userId") + "-src\"}"
                            ))
                            .check(status().is(201))
                            .check(jsonPath("$.id").saveAs("sourceWalletId"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(depositFeeder)
            .exec(
                    // 2. Depositar valor da massa na origem
                    http("Deposit to Source")
                            .post(session -> "/wallets/" + session.getLong("sourceWalletId") + "/deposit")
                            .body(StringBody(session ->
                                    "{\"amount\":" + session.getString("amount") + "}"
                            ))
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(walletFeeder)
            .exec(
                    // 3. Criar carteira de destino
                    http("Create Destination Wallet")
                            .post("/wallets")
                            .body(StringBody(session ->
                                    "{\"userId\":\"" + session.getString("userId") + "-dst\"}"
                            ))
                            .check(status().is(201))
                            .check(jsonPath("$.id").saveAs("destWalletId"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(pixKeyFeeder)
            .exec(
                    // 4. Registrar chave Pix no destino com dados da massa
                    http("Register Pix Key for Destination")
                            .post(session -> "/wallets/" + session.getLong("destWalletId") + "/pix-keys")
                            .body(StringBody(session -> {
                                String keyValue = session.getString("keyValue");
                                session.set("pixKey", keyValue);
                                return "{"
                                        + "\"keyType\":\"" + session.getString("keyType") + "\","
                                        + "\"keyValue\":\"" + keyValue + "\""
                                        + "}";
                            }))
                            .check(status().in(201, 409))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(transferFeeder)
            .exec(
                    // 5. Realizar transferência Pix com UUID dinâmico para idempotência
                    http("Pix Transfer")
                            .post("/pix/transfers")
                            .header("Idempotency-Key", session -> UUID.randomUUID().toString())
                            .body(StringBody(session ->
                                    "{"
                                    + "\"fromWalletId\":" + session.getLong("sourceWalletId") + ","
                                    + "\"pixKey\":\"" + session.getString("pixKey") + "\","
                                    + "\"amount\":" + session.getString("amount")
                                    + "}"
                            ))
                            .check(status().is(201))
                            .check(jsonPath("$.endToEndId").saveAs("endToEndId"))
                            .check(jsonPath("$.status").is("PENDING"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .exec(
                    // 6. Consultar saldo de origem
                    http("Verify Source Balance")
                            .get(session -> "/wallets/" + session.getLong("sourceWalletId") + "/balance")
                            .check(status().is(200))
                            .check(jsonPath("$.balance").exists())
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500));

    /**
     * Cenário 3: Processamento de Webhooks
     */
    private ScenarioBuilder webhookProcessingScenario = scenario("Webhook Processing")
            .feed(walletFeeder)
            .exec(
                    // 1. Criar carteira origem com dados da massa
                    http("Create Source Wallet")
                            .post("/wallets")
                            .body(StringBody(session ->
                                    "{\"userId\":\"" + session.getString("userId") + "-whsrc\"}"
                            ))
                            .check(status().is(201))
                            .check(jsonPath("$.id").saveAs("sourceWalletId"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(depositFeeder)
            .exec(
                    // 2. Depositar com valor da massa
                    http("Deposit")
                            .post(session -> "/wallets/" + session.getLong("sourceWalletId") + "/deposit")
                            .body(StringBody(session ->
                                    "{\"amount\":" + session.getString("amount") + "}"
                            ))
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(walletFeeder)
            .exec(
                    // 3. Criar carteira destino
                    http("Create Destination Wallet")
                            .post("/wallets")
                            .body(StringBody(session ->
                                    "{\"userId\":\"" + session.getString("userId") + "-whdst\"}"
                            ))
                            .check(status().is(201))
                            .check(jsonPath("$.id").saveAs("destWalletId"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(pixKeyFeeder)
            .exec(
                    // 4. Registrar chave Pix com dados da massa
                    http("Register Pix Key")
                            .post(session -> "/wallets/" + session.getLong("destWalletId") + "/pix-keys")
                            .body(StringBody(session -> {
                                String keyValue = session.getString("keyValue");
                                session.set("pixKey", keyValue);
                                return "{"
                                        + "\"keyType\":\"" + session.getString("keyType") + "\","
                                        + "\"keyValue\":\"" + keyValue + "\""
                                        + "}";
                            }))
                            .check(status().in(201, 409))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(transferFeeder)
            .exec(
                    // 5. Fazer transferência com UUID dinâmico
                    http("Pix Transfer")
                            .post("/pix/transfers")
                            .header("Idempotency-Key", session -> UUID.randomUUID().toString())
                            .body(StringBody(session ->
                                    "{"
                                    + "\"fromWalletId\":" + session.getLong("sourceWalletId") + ","
                                    + "\"pixKey\":\"" + session.getString("pixKey") + "\","
                                    + "\"amount\":" + session.getString("amount")
                                    + "}"
                            ))
                            .check(status().is(201))
                            .check(jsonPath("$.endToEndId").saveAs("endToEndId"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .exec(
                    // 6. Processar webhook com IDs dinâmicos do endToEndId salvo
                    http("Process Webhook")
                            .post("/pix/webhook")
                            .body(StringBody(session -> {
                                // 70% CONFIRMED, 30% REJECTED (baseado no hash do endToEndId)
                                String endToEndId = session.getString("endToEndId");
                                String eventType = (Math.abs(endToEndId.hashCode()) % 10 <= 6) ? "CONFIRMED" : "REJECTED";
                                return "{"
                                        + "\"endToEndId\":\"" + endToEndId + "\","
                                        + "\"eventId\":\"" + UUID.randomUUID().toString() + "\","
                                        + "\"eventType\":\"" + eventType + "\","
                                        + "\"occurredAt\":\"2025-10-18T10:00:00Z\""
                                        + "}";
                            }))
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500));

    /**
     * Cenário 4: Operações de Saque
     */
    private ScenarioBuilder withdrawalScenario = scenario("Withdrawal Operations")
            .feed(walletFeeder)
            .exec(
                    // 1. Criar carteira com dados da massa
                    http("Create Wallet")
                            .post("/wallets")
                            .body(StringBody(session ->
                                    "{\"userId\":\"" + session.getString("userId") + "-wd\"}"
                            ))
                            .check(status().is(201))
                            .check(jsonPath("$.id").saveAs("walletId"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(depositFeeder)
            .exec(
                    // 2. Depositar com valor da massa
                    http("Deposit")
                            .post(session -> "/wallets/" + session.getLong("walletId") + "/deposit")
                            .body(StringBody(session ->
                                    "{\"amount\":" + session.getString("amount") + "}"
                            ))
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .feed(withdrawalFeeder)
            .exec(
                    // 3. Sacar com valor da massa
                    http("Withdraw")
                            .post(session -> "/wallets/" + session.getLong("walletId") + "/withdraw")
                            .body(StringBody(session ->
                                    "{\"amount\":" + session.getString("amount") + "}"
                            ))
                            .check(status().is(200))
                            .check(jsonPath("$.balance").saveAs("finalBalance"))
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500))
            .exec(
                    // 4. Consultar saldo final
                    http("Get Final Balance")
                            .get(session -> "/wallets/" + session.getLong("walletId") + "/balance")
                            .check(status().is(200))
                            .check(jsonPath("$.balance").exists())
            )
            .pause(Duration.ofMillis(100), Duration.ofMillis(500));

    // ========== SETUP ==========

    {
        int walletUsers = (int) (USERS * 0.40);
        int pixUsers = (int) (USERS * 0.35);
        int webhookUsers = (int) (USERS * 0.15);
        int withdrawalUsers = (int) (USERS * 0.10);

        setUp(
                walletManagementScenario.injectOpen(
                        rampUsers(walletUsers).during(Duration.ofSeconds(RAMPUP_DURATION))
                ).protocols(httpProtocol),

                pixTransferScenario.injectOpen(
                        rampUsers(pixUsers).during(Duration.ofSeconds(RAMPUP_DURATION))
                ).protocols(httpProtocol),

                webhookProcessingScenario.injectOpen(
                        rampUsers(webhookUsers).during(Duration.ofSeconds(RAMPUP_DURATION))
                ).protocols(httpProtocol),

                withdrawalScenario.injectOpen(
                        rampUsers(withdrawalUsers).during(Duration.ofSeconds(RAMPUP_DURATION))
                ).protocols(httpProtocol)
        )
        .assertions(
                global().responseTime().percentile3().lt(2000),
                global().responseTime().percentile4().lt(5000)
        )
        .maxDuration(Duration.ofSeconds(TEST_DURATION + RAMPDOWN_DURATION));
    }
}
