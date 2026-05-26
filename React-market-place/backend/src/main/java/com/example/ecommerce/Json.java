package com.example.ecommerce;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static String readBody(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                body.append(buffer, 0, read);
            }
            return body.toString();
        }
    }

    public static Object parse(String json) {
        return new Parser(json).parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        return (List<Object>) list;
    }

    public static String stringify(Object value) {
        StringBuilder builder = new StringBuilder();
        writeValue(builder, value);
        return builder.toString();
    }

    private static void writeValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }

        if (value instanceof String string) {
            builder.append('"').append(escape(string)).append('"');
            return;
        }

        if (value instanceof Number number) {
            if (number instanceof BigDecimal bigDecimal) {
                builder.append(bigDecimal.stripTrailingZeros().toPlainString());
            } else {
                builder.append(number.toString());
            }
            return;
        }

        if (value instanceof Boolean bool) {
            builder.append(bool);
            return;
        }

        if (value instanceof Map<?, ?> map) {
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeValue(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                writeValue(builder, entry.getValue());
            }
            builder.append('}');
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            builder.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeValue(builder, item);
            }
            builder.append(']');
            return;
        }

        if (value.getClass().isArray()) {
            builder.append('[');
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                writeValue(builder, Array.get(value, i));
            }
            builder.append(']');
            return;
        }

        try {
            Map<String, Object> properties = new LinkedHashMap<>();
            for (PropertyDescriptor descriptor : Introspector.getBeanInfo(value.getClass(), Object.class).getPropertyDescriptors()) {
                if (descriptor.getReadMethod() != null) {
                    properties.put(descriptor.getName(), descriptor.getReadMethod().invoke(value));
                }
            }
            writeValue(builder, properties);
        } catch (ReflectiveOperationException | IntrospectionException exception) {
            throw new IllegalArgumentException("Unable to serialize " + value.getClass().getSimpleName(), exception);
        }
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= json.length()) {
                throw error("Unexpected end of input");
            }

            char ch = json.charAt(index);
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseTrue();
                case 'f' -> parseFalse();
                case 'n' -> parseNull();
                default -> {
                    if (ch == '-' || Character.isDigit(ch)) {
                        yield parseNumber();
                    }
                    throw error("Unexpected character: " + ch);
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return result;
            }

            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < json.length()) {
                char ch = json.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (index >= json.length()) {
                        throw error("Invalid escape sequence");
                    }
                    char escaped = json.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> builder.append(escaped);
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> {
                            if (index + 4 > json.length()) {
                                throw error("Invalid unicode escape");
                            }
                            String hex = json.substring(index, index + 4);
                            builder.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                        }
                        default -> throw error("Invalid escape character: " + escaped);
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw error("Unterminated string");
        }

        private BigDecimal parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            consumeDigits();
            if (peek('.')) {
                index++;
                consumeDigits();
            }
            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                consumeDigits();
            }
            return new BigDecimal(json.substring(start, index));
        }

        private Boolean parseTrue() {
            consumeLiteral("true");
            return Boolean.TRUE;
        }

        private Boolean parseFalse() {
            consumeLiteral("false");
            return Boolean.FALSE;
        }

        private Object parseNull() {
            consumeLiteral("null");
            return null;
        }

        private void consumeLiteral(String literal) {
            if (!json.startsWith(literal, index)) {
                throw error("Expected " + literal);
            }
            index += literal.length();
        }

        private void consumeDigits() {
            int start = index;
            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("Expected digits");
            }
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= json.length() || json.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < json.length() && json.charAt(index) == expected;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + index);
        }
    }
}

