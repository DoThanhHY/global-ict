package com.globalict.iot_backend.Dto;

import com.globalict.iot_backend.entity.Device;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDeviceRequest {
    @NotBlank
    private String deviceId;
    @NotBlank
    private String name;
    private String location;
    private Device.DeviceType type;
}
