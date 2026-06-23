package com.globalict.iot_backend.service;

import com.globalict.iot_backend.Dto.ThresholdAlertResponse;
import com.globalict.iot_backend.entity.ThresholdAlert;
import com.globalict.iot_backend.repository.ThresholdAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdAlertService {

    private final ThresholdAlertRepository thresholdAlertRepository;

    @Transactional(readOnly = true)
    public List<ThresholdAlertResponse> findByDeviceId(Long deviceId) {
        log.debug("Fetching threshold alerts for device: {}", deviceId);
        return thresholdAlertRepository.findByDeviceIdOrderByTriggeredAtDesc(deviceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ThresholdAlertResponse> findUnresolvedByDeviceId(Long deviceId) {
        log.debug("Fetching unresolved threshold alerts for device: {}", deviceId);
        return thresholdAlertRepository.findUnresolvedByDeviceId(deviceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ThresholdAlertResponse> findAllUnresolved() {
        log.debug("Fetching all unresolved threshold alerts");
        return thresholdAlertRepository.findAllUnresolved().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ThresholdAlertResponse> findByThresholdId(Long thresholdId) {
        log.debug("Fetching threshold alerts for threshold: {}", thresholdId);
        return thresholdAlertRepository.findByThresholdId(thresholdId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ThresholdAlertResponse> findByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Fetching threshold alerts between {} and {}", startTime, endTime);
        return thresholdAlertRepository.findByTriggeredAtBetween(startTime, endTime).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ThresholdAlertResponse resolveAlert(Long alertId) {
        log.info("Resolving alert: {}", alertId);

        ThresholdAlert alert = thresholdAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        alert.setIsResolved(true);
        ThresholdAlert saved = thresholdAlertRepository.save(alert);

        log.info("Alert resolved: id={}", alertId);
        return mapToResponse(saved);
    }

    private ThresholdAlertResponse mapToResponse(ThresholdAlert alert) {
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
