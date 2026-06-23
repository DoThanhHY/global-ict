package com.globalict.iot_backend.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateThresholdRequest {

    @NotNull(message = "deviceId is required")
    private Long deviceId;

    @NotBlank(message = "field is required")
    private String field;  // "temperature", "humidity", "doorOpen", "switchOn"

    private Double minValue;

    private Double maxValue;
}
