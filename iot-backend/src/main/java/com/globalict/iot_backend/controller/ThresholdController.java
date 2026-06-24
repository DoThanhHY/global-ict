package com.globalict.iot_backend.controller;

import com.globalict.iot_backend.Dto.CreateThresholdRequest;
import com.globalict.iot_backend.Dto.ThresholdAlertResponse;
import com.globalict.iot_backend.Dto.ThresholdResponse;
import com.globalict.iot_backend.Dto.UpdateThresholdRequest;
import com.globalict.iot_backend.service.ThresholdAlertService;
import com.globalict.iot_backend.service.ThresholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thresholds")
@RequiredArgsConstructor
public class ThresholdController {

    private final ThresholdService thresholdService;
    private final ThresholdAlertService thresholdAlertService;

    /**
     * Create a new threshold for a device
     */
    @PostMapping
    public ResponseEntity<ThresholdResponse> createThreshold(@RequestBody @Valid CreateThresholdRequest req) {
        ThresholdResponse response = thresholdService.createThreshold(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all thresholds for a device
     */
    @GetMapping
    public ResponseEntity<List<ThresholdResponse>> getThresholdsByDevice(@RequestParam Long deviceId) {
        List<ThresholdResponse> thresholds = thresholdService.findByDeviceId(deviceId);
        return ResponseEntity.ok(thresholds);
    }

    /**
     * Get a threshold by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ThresholdResponse> getThresholdById(@PathVariable Long id) {
        ThresholdResponse response = thresholdService.findById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a threshold
     */
    @PutMapping("/{id}")
    public ResponseEntity<ThresholdResponse> updateThreshold(
            @PathVariable Long id,
            @RequestBody @Valid UpdateThresholdRequest req) {
        ThresholdResponse response = thresholdService.updateThreshold(id, req);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a threshold
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThreshold(@PathVariable Long id) {
        thresholdService.deleteThreshold(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get threshold alerts for a device
     */
    @GetMapping("/alerts/device/{deviceId}")
    public ResponseEntity<List<ThresholdAlertResponse>> getAlertsByDevice(@PathVariable Long deviceId) {
        List<ThresholdAlertResponse> alerts = thresholdAlertService.findByDeviceId(deviceId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get unresolved threshold alerts for a device
     */
    @GetMapping("/alerts/device/{deviceId}/unresolved")
    public ResponseEntity<List<ThresholdAlertResponse>> getUnresolvedAlertsByDevice(@PathVariable Long deviceId) {
        List<ThresholdAlertResponse> alerts = thresholdAlertService.findUnresolvedByDeviceId(deviceId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get all unresolved threshold alerts (global)
     */
    @GetMapping("/alerts/unresolved")
    public ResponseEntity<List<ThresholdAlertResponse>> getAllUnresolvedAlerts() {
        List<ThresholdAlertResponse> alerts = thresholdAlertService.findAllUnresolved();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts for a specific threshold
     */
    @GetMapping("/alerts/threshold/{thresholdId}")
    public ResponseEntity<List<ThresholdAlertResponse>> getAlertsByThreshold(@PathVariable Long thresholdId) {
        List<ThresholdAlertResponse> alerts = thresholdAlertService.findByThresholdId(thresholdId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Resolve an alert
     */
    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<ThresholdAlertResponse> resolveAlert(@PathVariable Long alertId) {
        ThresholdAlertResponse response = thresholdAlertService.resolveAlert(alertId);
        return ResponseEntity.ok(response);
    }
}
