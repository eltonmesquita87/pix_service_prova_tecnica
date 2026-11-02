package com.elton.pixservice.infrastructure.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WalletResponse Tests")
class WalletResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode serialize(WalletResponse response) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(response));
    }

    @Test
    @DisplayName("WHOLD-20: a response built with only id, userId and balance has none of the four holder keys")
    void shouldOmitHolderKeysWhenAbsent() throws Exception {
        WalletResponse response = WalletResponse.builder()
                .id(1L)
                .userId("user123")
                .balance(new BigDecimal("10.50"))
                .build();

        JsonNode json = serialize(response);

        assertFalse(json.has("cpf"), json.toString());
        assertFalse(json.has("nome"), json.toString());
        assertFalse(json.has("email"), json.toString());
        assertFalse(json.has("telefone"), json.toString());
        assertEquals(3, json.size());
    }

    @Test
    @DisplayName("WHOLD-16/17: a response with the four holder fields serializes each of them")
    void shouldSerializeHolderFieldsWhenPresent() throws Exception {
        WalletResponse response = WalletResponse.builder()
                .id(1L)
                .userId("user123")
                .balance(BigDecimal.ZERO)
                .cpf("***.456.789-**")
                .nome("Maria Silva")
                .email("maria@example.com")
                .telefone("+5511999999999")
                .build();

        JsonNode json = serialize(response);

        assertEquals("***.456.789-**", json.get("cpf").asText());
        assertEquals("Maria Silva", json.get("nome").asText());
        assertEquals("maria@example.com", json.get("email").asText());
        assertEquals("+5511999999999", json.get("telefone").asText());
    }

    @Test
    @DisplayName("WHOLD-13: id, userId and balance serialize as before")
    void shouldKeepExistingFieldsUnchanged() throws Exception {
        WalletResponse response = WalletResponse.builder()
                .id(7L)
                .userId("user123")
                .balance(new BigDecimal("10.50"))
                .cpf("***.456.789-**")
                .build();

        JsonNode json = serialize(response);

        assertEquals(7L, json.get("id").asLong());
        assertEquals("user123", json.get("userId").asText());
        assertEquals(0, new BigDecimal("10.50").compareTo(json.get("balance").decimalValue()));
    }

    @Test
    @DisplayName("WHOLD-20: a holder field that is null is omitted while the others are kept")
    void shouldOmitOnlyTheNullHolderFields() throws Exception {
        WalletResponse response = WalletResponse.builder()
                .id(1L)
                .userId("user123")
                .balance(BigDecimal.ZERO)
                .nome("Maria Silva")
                .build();

        JsonNode json = serialize(response);

        assertEquals("Maria Silva", json.get("nome").asText());
        assertFalse(json.has("cpf"), json.toString());
        assertFalse(json.has("email"), json.toString());
        assertFalse(json.has("telefone"), json.toString());
    }
}
