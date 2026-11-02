package com.elton.pixservice.infrastructure.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.elton.pixservice.domain.exception.CpfJaCadastradoException;
import com.elton.pixservice.domain.exception.DadosTitularInvalidosException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private static final String CPF_DIGITS = "12345678909";
    private static final String CPF_MASKED = "***.456.789-**";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/wallets");

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).detachAppender(appender);
        appender.stop();
    }

    static Stream<Arguments> holderErrors() {
        return Stream.of(
                Arguments.of("WHOLD-02", DadosTitularInvalidosException.cpfInvalido(), "Invalid CPF"),
                Arguments.of("WHOLD-04", DadosTitularInvalidosException.emailInvalido(), "Invalid email"),
                Arguments.of("WHOLD-06", DadosTitularInvalidosException.telefoneInvalido(), "Invalid phone"),
                Arguments.of("WHOLD-07", DadosTitularInvalidosException.nomeInvalido(), "Invalid name"));
    }

    @ParameterizedTest(name = "{0}: {2} gives 400 Bad Request")
    @MethodSource("holderErrors")
    @DisplayName("WHOLD-02/04/06/07/09: DadosTitularInvalidosException gives 400 with its exact message")
    void shouldMapInvalidHolderDataTo400(String requirement, DadosTitularInvalidosException exception,
                                         String expectedMessage) {
        ResponseEntity<ErrorResponse> response = handler.handleDadosTitularInvalidos(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.getStatus());
        assertEquals("Bad Request", body.getError());
        assertEquals(expectedMessage, body.getMessage());
        assertEquals("/wallets", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    @Test
    @DisplayName("WHOLD-10: CpfJaCadastradoException gives 409 Conflict with 'CPF already registered'")
    void shouldMapDuplicateCpfExceptionTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleCpfJaCadastrado(new CpfJaCadastradoException(), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(409, body.getStatus());
        assertEquals("Conflict", body.getError());
        assertEquals("CPF already registered", body.getMessage());
        assertEquals("/wallets", body.getPath());
    }

    @Test
    @DisplayName("WHOLD-11/18/19: uk_wallets_cpf violation gives 409 'CPF already registered' without the CPF in body or logs")
    void shouldMapCpfUniqueViolationTo409WithoutLeakingCpf() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [uk_wallets_cpf]",
                new RuntimeException("ERROR: duplicate key value violates unique constraint \"uk_wallets_cpf\"\n"
                        + "  Detail: Key (cpf)=(" + CPF_DIGITS + ") already exists."));
        assertTrue(exception.getMessage().contains(CPF_DIGITS), "precondition: the raw exception carries the CPF");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(409, body.getStatus());
        assertEquals("Conflict", body.getError());
        assertEquals("CPF already registered", body.getMessage());
        assertFalse(body.toString().contains(CPF_DIGITS));
        assertFalse(body.toString().contains(CPF_MASKED));

        assertFalse(appender.list.isEmpty(), "the handler is expected to log, otherwise this test proves nothing");
        for (ILoggingEvent event : appender.list) {
            assertNoCpf(event);
            assertNull(event.getThrowableProxy(), "no stack trace may be logged for the CPF violation");
        }
    }

    @Test
    @DisplayName("WHOLD-18: uk_wallets_cpf violation is logged with a fixed text at warn level")
    void shouldLogFixedTextForCpfUniqueViolation() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "constraint [uk_wallets_cpf] Detail: Key (cpf)=(" + CPF_DIGITS + ") already exists.");

        handler.handleDataIntegrityViolationException(exception, request);

        assertEquals(1, appender.list.size());
        assertEquals("CPF uniqueness violation", appender.list.get(0).getFormattedMessage());
        assertEquals("WARN", appender.list.get(0).getLevel().toString());
    }

    @Test
    @DisplayName("Regression: uk_pix_key_value violation keeps 409 'Pix key already registered'")
    void shouldKeepPixKeyUniqueViolationMessage() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement; constraint [uk_pix_key_value]");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("Pix key already registered", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Regression: uk_scope_key violation keeps 409 'Duplicate request detected'")
    void shouldKeepIdempotencyUniqueViolationMessage() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement; constraint [uk_scope_key]");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("Duplicate request detected", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Regression: other integrity violations keep 409 'Data integrity violation'")
    void shouldKeepGenericIntegrityViolationMessage() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement; constraint [fk_something_else]");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().getError());
        assertEquals("Data integrity violation", response.getBody().getMessage());
    }

    @Test
    @DisplayName("WHOLD-19: logs of the holder error handlers contain no personal data")
    void shouldNotLogPersonalDataForHolderErrors() {
        handler.handleDadosTitularInvalidos(DadosTitularInvalidosException.cpfInvalido(), request);
        handler.handleCpfJaCadastrado(new CpfJaCadastradoException(), request);

        assertEquals(2, appender.list.size());
        for (ILoggingEvent event : appender.list) {
            assertNoCpf(event);
        }
    }

    private void assertNoCpf(ILoggingEvent event) {
        String formatted = event.getFormattedMessage();
        assertFalse(formatted.contains(CPF_DIGITS), formatted);
        assertFalse(formatted.contains(CPF_MASKED), formatted);
        if (event.getThrowableProxy() != null) {
            String throwable = event.getThrowableProxy().getMessage()
                    + ThrowableProxyUtil.asString(event.getThrowableProxy());
            assertFalse(throwable.contains(CPF_DIGITS), throwable);
            assertFalse(throwable.contains(CPF_MASKED), throwable);
        }
    }
}
