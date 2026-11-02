package com.elton.pixservice.infrastructure.web.dto;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateWalletRequest Tests")
class CreateWalletRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("WHOLD-13: a body with only userId deserializes with the four holder fields null")
    void shouldDeserializeBodyWithOnlyUserId() throws Exception {
        CreateWalletRequest request = objectMapper.readValue("{\"userId\":\"u\"}", CreateWalletRequest.class);

        assertEquals("u", request.getUserId());
        assertNull(request.getCpf());
        assertNull(request.getNome());
        assertNull(request.getEmail());
        assertNull(request.getTelefone());
    }

    @Test
    @DisplayName("WHOLD-13: the one-argument constructor behaves like a body with only userId")
    void shouldKeepOneArgumentConstructor() throws Exception {
        CreateWalletRequest fromConstructor = new CreateWalletRequest("u");
        CreateWalletRequest fromJson = objectMapper.readValue("{\"userId\":\"u\"}", CreateWalletRequest.class);

        assertEquals("u", fromConstructor.getUserId());
        assertNull(fromConstructor.getCpf());
        assertNull(fromConstructor.getNome());
        assertNull(fromConstructor.getEmail());
        assertNull(fromConstructor.getTelefone());
        assertEquals(fromJson, fromConstructor);
    }

    @Test
    @DisplayName("WHOLD-01: a body with the four holder fields as strings deserializes all of them as sent")
    void shouldDeserializeFullBody() throws Exception {
        String json = "{\"userId\":\"u\",\"cpf\":\"123.456.789-09\",\"nome\":\" Maria Silva \","
                + "\"email\":\"Maria@Example.com\",\"telefone\":\"+5511999999999\"}";

        CreateWalletRequest request = objectMapper.readValue(json, CreateWalletRequest.class);

        assertEquals("u", request.getUserId());
        assertEquals("123.456.789-09", request.getCpf());
        assertEquals(" Maria Silva ", request.getNome());
        assertEquals("Maria@Example.com", request.getEmail());
        assertEquals("+5511999999999", request.getTelefone());
    }

    @Test
    @DisplayName("Edge case (explicit null): a null holder field deserializes as absent")
    void shouldTreatExplicitNullAsAbsent() throws Exception {
        CreateWalletRequest request = objectMapper.readValue(
                "{\"userId\":\"u\",\"cpf\":null,\"nome\":null,\"email\":null,\"telefone\":null}",
                CreateWalletRequest.class);

        assertNull(request.getCpf());
        assertNull(request.getNome());
        assertNull(request.getEmail());
        assertNull(request.getTelefone());
    }

    @ParameterizedTest(name = "{0} as a JSON number fails deserialization")
    @ValueSource(strings = {"cpf", "nome", "email", "telefone"})
    @DisplayName("Edge case (cpf with wrong JSON type): a numeric holder field fails deserialization")
    void shouldRejectNumericHolderFields(String field) {
        String json = "{\"userId\":\"u\",\"" + field + "\":12345678909}";

        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(json, CreateWalletRequest.class));
    }

    @Test
    @DisplayName("WHOLD-18: toString contains no CPF, e-mail, phone or name")
    void shouldNotExposeHolderDataInToString() {
        CreateWalletRequest request = new CreateWalletRequest(
                "u", "12345678909", "Maria Silva", "maria@example.com", "+5511999999999");

        String text = request.toString();

        assertFalse(text.contains("12345678909"), text);
        assertFalse(text.contains("Maria Silva"), text);
        assertFalse(text.contains("maria@example.com"), text);
        assertFalse(text.contains("+5511999999999"), text);
        assertTrue(text.contains("userId=u"), text);
    }

    @Test
    @DisplayName("WHOLD-14: a blank userId still fails validation with 'User ID is required'")
    void shouldKeepUserIdRequired() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Set<ConstraintViolation<CreateWalletRequest>> violations = validator.validate(new CreateWalletRequest(" "));

        assertEquals(1, violations.size());
        assertEquals("User ID is required", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("AD-001: holder fields carry no Bean Validation, so format errors are left to the domain")
    void shouldNotValidateHolderFormatInTheDto() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        CreateWalletRequest request = new CreateWalletRequest("u", "not-a-cpf", "x", "not-an-email", "123");

        assertTrue(validator.validate(request).isEmpty());
    }
}
