package com.globalict.iot_backend.controller;

import com.globalict.iot_backend.Dto.SensorDataResponse;
import com.globalict.iot_backend.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

// import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sensor-data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SensorDataController {

    private final SensorDataRepository sensorDataRepository;

    // Lấy 50 bản ghi mới nhất của 1 device
    @GetMapping("/device/{deviceId}")
    public List<SensorDataResponse> getByDevice(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "50") int limit) {
        return sensorDataRepository
                .findLatestByDeviceId(deviceId, PageRequest.of(0, limit))
                .stream()
                .map(SensorDataResponse::from)
                .toList();
    }

    // Stats dashboard
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return sensorDataRepository.getDashboardStats(LocalDateTime.now().minusHours(24));
    }
}