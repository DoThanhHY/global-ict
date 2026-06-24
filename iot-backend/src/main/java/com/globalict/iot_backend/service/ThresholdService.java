package com.globalict.iot_backend.service;

import com.globalict.iot_backend.Dto.CreateThresholdRequest;
import com.globalict.iot_backend.Dto.ThresholdAlertResponse;
import com.globalict.iot_backend.Dto.ThresholdResponse;
import com.globalict.iot_backend.Dto.UpdateThresholdRequest;
import com.globalict.iot_backend.entity.Device;
import com.globalict.iot_backend.entity.SensorData;
import com.globalict.iot_backend.entity.Threshold;
import com.globalict.iot_backend.entity.ThresholdAlert;
import com.globalict.iot_backend.repository.DeviceRepository;
import com.globalict.iot_backend.repository.ThresholdAlertRepository;
import com.globalict.iot_backend.repository.ThresholdRepository;
import com.globalict.iot_backend.exception.ConflictException;
import com.globalict.iot_backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdService {

    private final ThresholdRepository thresholdRepository;
    private final ThresholdAlertRepository thresholdAlertRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public ThresholdResponse createThreshold(CreateThresholdRequest req) {
        log.info("Creating threshold for device: {}, field: {}", req.getDeviceId(), req.getField());

        Device device = deviceRepository.findById(req.getDeviceId())
                .orElseThrow(() -> {
                    log.error("Device not found: {}", req.getDeviceId());
                    return new ResourceNotFoundException("Device not found: " + req.getDeviceId());
                });

        // Check if threshold already exists for this field
        thresholdRepository.findByDeviceIdAndField(req.getDeviceId(), req.getField())
                .ifPresent(t -> {
                    log.warn("Threshold already exists for device: {}, field: {}", req.getDeviceId(), req.getField());
                    throw new ConflictException("Threshold already exists for field: " + req.getField());
                });

        Threshold threshold = Threshold.builder()
                .device(device)
                .field(req.getField())
                .minValue(req.getMinValue())
                .maxValue(req.getMaxValue())
                .build();

        Threshold saved = thresholdRepository.save(threshold);
        log.info("Threshold created successfully: id={}, device={}, field={}", saved.getId(), device.getDeviceId(), req.getField());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ThresholdResponse> findByDeviceId(Long deviceId) {
        log.debug("Fetching thresholds for device: {}", deviceId);
        return thresholdRepository.findByDeviceId(deviceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ThresholdResponse findById(Long id) {
        log.debug("Fetching threshold: {}", id);
        Threshold threshold = thresholdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Threshold not found: " + id));
        return mapToResponse(threshold);
    }

    @Transactional
    public ThresholdResponse updateThreshold(Long id, UpdateThresholdRequest req) {
        log.info("Updating threshold: {}", id);

        Threshold threshold = thresholdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Threshold not found: " + id));

        if (req.getField() != null) {
            threshold.setField(req.getField());
        }
        if (req.getMinValue() != null) {
            threshold.setMinValue(req.getMinValue());
        }
        if (req.getMaxValue() != null) {
            threshold.setMaxValue(req.getMaxValue());
        }

        Threshold updated = thresholdRepository.save(threshold);
        log.info("Threshold updated: id={}", id);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteThreshold(Long id) {
        log.info("Deleting threshold: {}", id);

        Threshold threshold = thresholdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Threshold not found: " + id));

        thresholdRepository.delete(threshold);
        log.info("Threshold deleted: id={}", id);
    }

    /**
     * Validate sensor data against all thresholds for the device
     * @param sensorData sensor data to validate
     * @return list of threshold alerts triggered
     */
    public List<ThresholdAlertResponse> validateSensorData(SensorData sensorData) {
        List<ThresholdAlertResponse> alerts = new ArrayList<>();

        if (sensorData.getDevice() == null) {
            log.warn("SensorData has no device associated");
            return alerts;
        }

        List<Threshold> thresholds = thresholdRepository.findByDeviceId(sensorData.getDevice().getId());

        for (Threshold threshold : thresholds) {
            Double value = getFieldValue(sensorData, threshold.getField());

            if (value == null) {
                continue;
            }

            ThresholdAlert alert = null;

            // Check min threshold
            if (threshold.getMinValue() != null && value < threshold.getMinValue()) {
                alert = createAlert(threshold, sensorData, value, "MIN_EXCEEDED", threshold.getMinValue());
                log.warn("Threshold alert: device={}, field={}, value={}, min={}, type=MIN_EXCEEDED",
                        sensorData.getDevice().getDeviceId(), threshold.getField(), value, threshold.getMinValue());
            }
            // Check max threshold
            else if (threshold.getMaxValue() != null && value > threshold.getMaxValue()) {
                alert = createAlert(threshold, sensorData, value, "MAX_EXCEEDED", threshold.getMaxValue());
                log.warn("Threshold alert: device={}, field={}, value={}, max={}, type=MAX_EXCEEDED",
                        sensorData.getDevice().getDeviceId(), threshold.getField(), value, threshold.getMaxValue());
            }

            if (alert != null) {
                ThresholdAlert saved = thresholdAlertRepository.save(alert);
                alerts.add(mapAlertToResponse(saved));
            }
        }

        return alerts;
    }

    private Double getFieldValue(SensorData sensorData, String field) {
        return switch (field.toLowerCase()) {
            case "temperature" -> sensorData.getTemperature();
            case "humidity" -> sensorData.getHumidity();
            default -> null;
        };
    }

    private ThresholdAlert createAlert(Threshold threshold, SensorData sensorData, Double actualValue,
                                       String alertType, Double thresholdValue) {
        return ThresholdAlert.builder()
                .threshold(threshold)
                .device(sensorData.getDevice())
                .fieldName(threshold.getField())
                .actualValue(actualValue)
                .alertType(alertType)
                .thresholdValue(thresholdValue)
                .isResolved(false)
                .build();
    }

    private ThresholdResponse mapToResponse(Threshold threshold) {
        return ThresholdResponse.builder()
                .id(threshold.getId())
                .deviceId(threshold.getDevice().getId())
                .deviceName(threshold.getDevice().getName())
                .field(threshold.getField())
                .minValue(threshold.getMinValue())
                .maxValue(threshold.getMaxValue())
                .createdAt(threshold.getCreatedAt())
                .updatedAt(threshold.getUpdatedAt())
                .build();
    }

    private ThresholdAlertResponse mapAlertToResponse(ThresholdAlert alert) {
        return ThresholdAlertResponse.builder()
                .id(alert.getId())
                .thresholdId(alert.getThreshold().getId())
                .deviceId(alert.getDevice().getId())
                .deviceName(alert.getDevice().getName())
                .fieldName(alert.getFieldName())
                .actualValue(alert.getActualValue())
                .thresholdValue(alert.getThresholdValue())
                .alertType(alert.getAlertType())
                .triggeredAt(alert.getTriggeredAt())
                .isResolved(alert.getIsResolved())
                .build();
    }
}
