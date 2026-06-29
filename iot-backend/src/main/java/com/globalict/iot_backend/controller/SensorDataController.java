package com.globalict.iot_backend.controller;

import com.globalict.iot_backend.Dto.SensorDataResponse;
import com.globalict.iot_backend.repository.SensorDataRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensor-data")
@RequiredArgsConstructor
@Validated
public class SensorDataController {

    private final SensorDataRepository sensorDataRepository;

    @GetMapping("/device/{deviceId}")
    public List<SensorDataResponse> getByDevice(
            @PathVariable String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int limit) {
        LocalDateTime fromLocal = from == null
            ? null
            : from.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toLocal = to == null
            ? null
            : to.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        if (fromLocal == null && toLocal == null) {
            return sensorDataRepository
                .findLatestByDeviceId(deviceId, PageRequest.of(0, limit))
                .stream()
                .map(SensorDataResponse::from)
                .toList();
        }

        if (fromLocal != null && toLocal != null) {
            return sensorDataRepository
                .findByDeviceIdAndRecordedAtBetween(deviceId, fromLocal, toLocal)
                .stream()
                .map(SensorDataResponse::from)
                .toList();
        }

        if (fromLocal != null) {
            return sensorDataRepository
                .findByDeviceIdAndRecordedAtAfter(deviceId, fromLocal)
                .stream()
                .map(SensorDataResponse::from)
                .toList();
        }

        return sensorDataRepository
            .findByDeviceIdAndRecordedAtBefore(deviceId, toLocal)
            .stream()
            .map(SensorDataResponse::from)
            .toList();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return sensorDataRepository.getDashboardStats(LocalDateTime.now().minusHours(24));
    }
}
