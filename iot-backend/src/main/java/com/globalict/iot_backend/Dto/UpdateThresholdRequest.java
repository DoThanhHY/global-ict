package com.globalict.iot_backend.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateThresholdRequest {

    private String field;

    private Double minValue;

    private Double maxValue;
}
