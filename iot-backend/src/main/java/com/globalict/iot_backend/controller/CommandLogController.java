package com.globalict.iot_backend.controller;

import com.globalict.iot_backend.Dto.CommandLogResponse;
import com.globalict.iot_backend.entity.CommandLog;
import com.globalict.iot_backend.repository.CommandLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/command-logs")
@RequiredArgsConstructor
public class CommandLogController {

    private final CommandLogRepository commandLogRepository;

    @GetMapping
    public ResponseEntity<List<CommandLogResponse>> getAll() {
        List<CommandLog> logs = commandLogRepository.findAllByOrderBySentAtDesc();
        return ResponseEntity.ok(toResponseList(logs));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<CommandLogResponse>> getByDevice(@PathVariable String deviceId) {
        List<CommandLog> logs = commandLogRepository.findAllByDeviceIdOrderBySentAtDesc(deviceId);
        return ResponseEntity.ok(toResponseList(logs));
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<CommandLogResponse>> getByUser(@PathVariable String username) {
        List<CommandLog> logs = commandLogRepository.findAllBySentByOrderBySentAtDesc(username);
        return ResponseEntity.ok(toResponseList(logs));
    }

    private List<CommandLogResponse> toResponseList(List<CommandLog> logs) {
        return logs.stream().map(log -> CommandLogResponse.builder()
                .id(log.getId())
                .deviceId(log.getDeviceId())
                .action(log.getAction())
                .sentBy(log.getSentBy())
                .sentAt(log.getSentAt())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .build()).toList();
    }
}
