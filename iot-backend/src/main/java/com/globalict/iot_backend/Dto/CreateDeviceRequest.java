package com.globalict.iot_backend.Dto;

import com.globalict.iot_backend.entity.Device;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDeviceRequest {
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "deviceId must contain only alphanumeric characters, dashes, or underscores")
    private String deviceId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 200)
    private String location;

    private Device.DeviceType type;
}
