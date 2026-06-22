package com.globalict.iot_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data @Builder @NoArgsConstructor @AllArgsConstructor

public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String deviceId; // "esp32-001"

    private String name;
    private String location;

    @Enumerated(EnumType.STRING)
    private DeviceType type; // TEMPERATURE_HUMIDITY, DOOR_SENSOR, SWITCH\

    private boolean online = false;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;

    public enum DeviceType {
        TEMPERATURE_HUMIDITY, DOOR_SENSOR, SWITCH
    }
}