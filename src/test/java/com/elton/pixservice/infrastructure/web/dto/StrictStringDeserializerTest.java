package com.elton.pixservice.infrastructure.web.dto;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StrictStringDeserializer Tests")
class StrictStringDeserializerTest {

    static class Holder {
        @JsonDeserialize(using = StrictStringDeserializer.class)
        public String value;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Holder read(String json) throws Exception {
        return objectMapper.readValue(json, Holder.class);
    }

    @Test
    @DisplayName("Edge case (cpf with wrong JSON type): a JSON string is deserialized unchanged")
    void shouldAcceptJsonString() throws Exception {
        assertEquals("abc", read("{\"value\":\"abc\"}").value);
    }

    @Test
    @DisplayName("Edge case (cpf with wrong JSON type): a numeric-looking JSON string is kept as the string")
    void shouldKeepNumericLookingString() throws Exception {
        assertEquals("12345678909", read("{\"value\":\"12345678909\"}").value);
    }

    @Test
    @DisplayName("Edge case (explicit null): JSON null gives null")
    void shouldAcceptJsonNull() throws Exception {
        assertNull(read("{\"value\":null}").value);
    }

    @Test
    @DisplayName("Edge case (cpf with wrong JSON type): a JSON number is rejected")
    void shouldRejectJsonNumber() {
        assertThrows(JsonMappingException.class, () -> read("{\"value\":123}"));
    }

    @Test
    @DisplayName("Edge case (cpf with wrong JSON type): a JSON boolean is rejected")
    void shouldRejectJsonBoolean() {
        assertThrows(JsonMappingException.class, () -> read("{\"value\":true}"));
    }

    @Test
    @DisplayName("Edge case (cpf with wrong JSON type): a JSON object is rejected")
    void shouldRejectJsonObject() {
        assertThrows(JsonMappingException.class, () -> read("{\"value\":{}}"));
    }

    @Test
    @DisplayName("Edge case (cpf with wrong JSON type): a JSON array is rejected")
    void shouldRejectJsonArray() {
        assertThrows(JsonMappingException.class, () -> read("{\"value\":[]}"));
    }

    @Test
    @DisplayName("Edge case (cpf with wrong JSON type): a JSON decimal number is rejected")
    void shouldRejectJsonDecimal() {
        assertThrows(JsonMappingException.class, () -> read("{\"value\":1.5}"));
    }
}
