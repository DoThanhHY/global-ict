package com.globalict.iot_backend.controller;

import com.globalict.iot_backend.Dto.CreateDeviceRequest;
import com.globalict.iot_backend.Dto.DeviceCommandRequest;
import com.globalict.iot_backend.Dto.UpdateDeviceRequest;
import com.globalict.iot_backend.entity.Device;
import com.globalict.iot_backend.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public List<Device> getAll() {
        return deviceService.findAll();
    }

    @PostMapping
    public Device create(@RequestBody @Valid CreateDeviceRequest req) {
        return deviceService.create(req);
    }

    @PutMapping("/{id}")
    public Device update(@PathVariable Long id, @RequestBody @Valid UpdateDeviceRequest req) {
        return deviceService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        deviceService.delete(id);
    }

    @PostMapping("/{deviceId}/command")
    public ResponseEntity<Void> sendCommand(
            @PathVariable String deviceId,
            @RequestBody @Valid DeviceCommandRequest req) {
        deviceService.sendCommand(deviceId, Map.of("action", req.getAction()));
        return ResponseEntity.ok().build();
    }
}
