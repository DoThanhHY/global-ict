package com.globalict.iot_backend.Dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
        regexp = "^(temperature|humidity|doorOpen|switchOn)$",
        message = "field must be one of: temperature, humidity, doorOpen, switchOn"
    )
    private String field;

    private Double minValue;

    private Double maxValue;

    @AssertTrue(message = "At least one of minValue or maxValue must be set")
    private boolean isAtLeastOneBoundSet() {
        return minValue != null || maxValue != null;
    }

    @AssertTrue(message = "minValue must be less than maxValue")
    private boolean isMinLessThanMax() {
        return minValue == null || maxValue == null || minValue < maxValue;
    }
}
