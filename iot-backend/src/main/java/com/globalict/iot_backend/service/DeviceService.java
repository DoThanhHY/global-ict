package com.globalict.iot_backend.service;

import com.globalict.iot_backend.Dto.CreateDeviceRequest;
import com.globalict.iot_backend.Dto.UpdateDeviceRequest;
import com.globalict.iot_backend.entity.Device;
import com.globalict.iot_backend.entity.CommandLog;
import com.globalict.iot_backend.repository.CommandLogRepository;
import com.globalict.iot_backend.repository.DeviceRepository;
import com.globalict.iot_backend.exception.ConflictException;
import com.globalict.iot_backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final CommandLogRepository commandLogRepository;
    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    public List<Device> findAll() {
        return deviceRepository.findAll();
    }

    public Device create(CreateDeviceRequest req) {
        if (deviceRepository.existsByDeviceId(req.getDeviceId())) {
            throw new ConflictException("DeviceId already exists: " + req.getDeviceId());
        }

        Device device = Device.builder()
                .deviceId(req.getDeviceId())
                .name(req.getName())
                .location(req.getLocation())
                .type(req.getType())
                .online(false)
                .createdAt(LocalDateTime.now())
                .build();

        return deviceRepository.save(device);
    }

    public Device update(Long id, UpdateDeviceRequest req) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + id));

        if (req.getName() != null) device.setName(req.getName());
        if (req.getLocation() != null) device.setLocation(req.getLocation());

        return deviceRepository.save(device);
    }

    public void delete(Long id) {
        deviceRepository.deleteById(id);
    }

    // Gọi khi nhận MQTT message — upsert device và đánh dấu online
    public Device markOnline(String deviceId, String deviceTypeRaw) {
        Device.DeviceType deviceType = parseDeviceType(deviceTypeRaw);

        return deviceRepository.findByDeviceId(deviceId)
                .map(device -> {
                    device.setOnline(true);
                    device.setLastSeen(LocalDateTime.now());
                    if (deviceType != null) {
                        device.setType(deviceType);
                    }
                    return deviceRepository.save(device);
                })
                .orElseGet(() -> {
                    // Auto-register nếu device chưa có trong DB
                    log.info("Auto-registering new device: {}", deviceId);
                    Device newDevice = Device.builder()
                            .deviceId(deviceId)
                            .name("Device " + deviceId)
                            .type(deviceType)
                            .online(true)
                            .lastSeen(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .build();
                    return deviceRepository.save(newDevice);
                });
    }

    private Device.DeviceType parseDeviceType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return null;
        }

        try {
            return Device.DeviceType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring invalid device type from payload: {}", rawType);
            return null;
        }
    }

    // Gửi command xuống ESP32 qua MQTT
    @Transactional
    public void sendCommand(String deviceId, Map<String, Object> command) {
        String action = (String) command.get("action");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            String topic = "home/" + deviceId + "/command";
            String payload = objectMapper.writeValueAsString(command);

            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            mqttClient.publish(topic, message);

            log.info("Command sent [{}]: {}", topic, payload);

            commandLogRepository.save(CommandLog.builder()
                    .deviceId(deviceId)
                    .action(action)
                    .sentBy(username)
                    .sentAt(LocalDateTime.now())
                    .status("SUCCESS")
                    .build());
        } catch (Exception e) {
            log.error("Failed to send command to device {}", deviceId, e);

            commandLogRepository.save(CommandLog.builder()
                    .deviceId(deviceId)
                    .action(action)
                    .sentBy(username)
                    .sentAt(LocalDateTime.now())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build());

            throw new RuntimeException("Không thể gửi lệnh tới thiết bị");
        }
    }
}