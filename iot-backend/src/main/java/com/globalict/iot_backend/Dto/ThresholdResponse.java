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
public class ThresholdResponse {

    private Long id;

    private Long deviceId;

    private String deviceName;

    private String field;

    private Double minValue;

    private Double maxValue;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
