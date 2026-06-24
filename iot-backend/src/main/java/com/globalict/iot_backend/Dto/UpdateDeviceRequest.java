package com.globalict.iot_backend.Dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDeviceRequest {
    @Size(max = 100)
    private String name;

    @Size(max = 200)
    private String location;
}
