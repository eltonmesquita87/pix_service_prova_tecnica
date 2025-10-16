package com.elton.pixservice.infrastructure;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for integration tests using REST Assured.
 *
 * Features:
 * - Spring Boot context with random port
 * - REST Assured configured
 * - Transactional rollback for database isolation
 * - Request/Response logging for debugging
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    protected RequestSpecification requestSpec;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
    }

    /**
     * Helper method to create request with idempotency key
     */
    protected RequestSpecification withIdempotencyKey(String key) {
        return RestAssured.given()
                .spec(requestSpec)
                .header("Idempotency-Key", key);
    }

    /**
     * Helper method to create standard request
     */
    protected RequestSpecification given() {
        return RestAssured.given().spec(requestSpec);
    }
}
