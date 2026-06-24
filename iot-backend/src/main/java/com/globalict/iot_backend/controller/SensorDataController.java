package com.globalict.iot_backend.controller;

import com.globalict.iot_backend.Dto.SensorDataResponse;
import com.globalict.iot_backend.repository.SensorDataRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensor-data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
public class SensorDataController {

    private final SensorDataRepository sensorDataRepository;

    @GetMapping("/device/{deviceId}")
    public List<SensorDataResponse> getByDevice(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(1000) int limit) {
        return sensorDataRepository
                .findLatestByDeviceId(deviceId, PageRequest.of(0, limit))
                .stream()
                .map(SensorDataResponse::from)
                .toList();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return sensorDataRepository.getDashboardStats(LocalDateTime.now().minusHours(24));
    }
}
