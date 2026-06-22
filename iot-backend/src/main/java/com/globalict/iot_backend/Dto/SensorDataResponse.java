package com.globalict.iot_backend.Dto;

import com.globalict.iot_backend.entity.SensorData;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SensorDataResponse {
    private Long id;
    private String deviceId;
    private Double temperature;
    private Double humidity;
    private Boolean doorOpen;
    private Boolean switchOn;
    private LocalDateTime recordedAt;

    public static SensorDataResponse from(SensorData s) {
        return SensorDataResponse.builder()
                .id(s.getId())
                .deviceId(s.getDevice().getDeviceId())
                .temperature(s.getTemperature())
                .humidity(s.getHumidity())
                .doorOpen(s.getDoorOpen())
                .switchOn(s.getSwitchOn())
                .recordedAt(s.getRecordedAt())
                .build();
    }
}