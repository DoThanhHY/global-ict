package com.globalict.iot_backend.util;

import com.globalict.iot_backend.exception.ValidationException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.regex.Pattern;

@Component
public class MqttPayloadValidator {

    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,50}$");
    private static final double TEMPERATURE_MIN = -80.0;
    private static final double TEMPERATURE_MAX = 150.0;
    private static final double HUMIDITY_MIN = 0.0;
    private static final double HUMIDITY_MAX = 100.0;

    public void validate(JsonNode json) {
        validateDeviceId(json);
        validateHasSensorField(json);
        validateTemperature(json);
        validateHumidity(json);
    }

    private void validateDeviceId(JsonNode json) {
        JsonNode node = json.get("deviceId");
        if (node == null || node.asString().isBlank()) {
            throw new ValidationException("MQTT payload missing deviceId");
        }
        if (!DEVICE_ID_PATTERN.matcher(node.asString()).matches()) {
            throw new ValidationException("Invalid deviceId format: " + node.asString());
        }
    }

    private void validateHasSensorField(JsonNode json) {
        if (!json.has("temperature") && !json.has("open") && !json.has("on")) {
            throw new ValidationException("MQTT payload must contain at least one sensor field");
        }
    }

    private void validateTemperature(JsonNode json) {
        if (!json.has("temperature")) return;
        double value = json.get("temperature").asDouble();
        if (value < TEMPERATURE_MIN || value > TEMPERATURE_MAX) {
            throw new ValidationException(
                "temperature out of range [" + TEMPERATURE_MIN + ", " + TEMPERATURE_MAX + "]: " + value);
        }
    }

    private void validateHumidity(JsonNode json) {
        if (!json.has("humidity")) return;
        double value = json.get("humidity").asDouble();
        if (value < HUMIDITY_MIN || value > HUMIDITY_MAX) {
            throw new ValidationException(
                "humidity out of range [" + HUMIDITY_MIN + ", " + HUMIDITY_MAX + "]: " + value);
        }
    }
}
