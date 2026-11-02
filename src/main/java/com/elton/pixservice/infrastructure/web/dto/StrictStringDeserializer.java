package com.elton.pixservice.infrastructure.web.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Deserializes only JSON strings into a String.
 * <p>
 * Jackson coerces numbers and booleans into String by default, which would accept
 * {@code "cpf": 12345678909}. This deserializer fails on anything but a JSON string;
 * a JSON {@code null} never reaches it and yields {@code null}.
 */
public class StrictStringDeserializer extends StdDeserializer<String> {

    public StrictStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_STRING)) {
            return parser.getText();
        }
        return (String) context.handleUnexpectedToken(String.class, parser);
    }
}
