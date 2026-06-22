package com.globalict.iot_backend.service;

import com.globalict.iot_backend.entity.Device;
import com.globalict.iot_backend.entity.SensorData;
import com.globalict.iot_backend.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j

public class MqttSubscriberService implements ApplicationRunner {

    private final MqttClient mqttClient;
    private final DeviceService deviceService;
    private final SensorDataRepository sensorDataRepository;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Subscribe tất cả topic home/+/data
        mqttClient.subscribe("home/+/data", (topic, message) -> {
            String payload = new String(message.getPayload());
            log.info("MQTT received [{}]: {}", topic, payload);

            try {
                JsonNode json = objectMapper.readTree(payload);
                String deviceId = json.get("deviceId").asText();

                // Upsert device (online)
                Device device = deviceService.markOnline(deviceId);

                // Lưu sensor data
                SensorData data = SensorData.builder()
                        .device(device)
                        .recordedAt(LocalDateTime.now())
                        .build();

                if (json.has("temperature")) {
                    data.setTemperature(json.get("temperature").asDouble());
                    data.setHumidity(json.get("humidity").asDouble());
                }
                if (json.has("open")) {
                    data.setDoorOpen(json.get("open").asBoolean());
                }
                if (json.has("on")) {
                    data.setSwitchOn(json.get("on").asBoolean());
                }

                sensorDataRepository.save(data);

                // Push real-time lên FE qua WebSocket
                webSocketService.sendToAll("/topic/devices/" + deviceId, payload);

            } catch (Exception e) {
                log.error("Error processing MQTT message", e);
            }
        });

        log.info("✅ MQTT subscribed to home/+/data");
    }
}