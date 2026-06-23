package com.globalict.iot_backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdAlertResponse {

    private Long id;

    private Long thresholdId;

    private Long deviceId;

    private String deviceName;

    private String fieldName;

    private Double actualValue;

    private Double thresholdValue;

    private String alertType;  // "MIN_EXCEEDED", "MAX_EXCEEDED"

    private LocalDateTime triggeredAt;

    private Boolean isResolved;
}
