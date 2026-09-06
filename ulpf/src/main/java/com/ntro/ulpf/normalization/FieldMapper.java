package com.ntro.ulpf.normalization;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FieldMapper {

    public String getString(
            Map<String, Object> fields,
            String... aliases
    ) {

        for (String alias : aliases) {

            Object value = fields.get(alias);

            if (value != null) {
                return String.valueOf(value);
            }
        }

        return null;
    }

    public Integer getInteger(
            Map<String, Object> fields,
            String... aliases
    ) {

        String value = getString(fields, aliases);

        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}