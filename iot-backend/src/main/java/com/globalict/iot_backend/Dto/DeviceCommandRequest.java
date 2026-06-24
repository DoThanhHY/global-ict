package com.globalict.iot_backend.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DeviceCommandRequest {
    @NotBlank
    @Pattern(
        regexp = "^(on|off|open|close|reset)$",
        message = "action must be one of: on, off, open, close, reset"
    )
    private String action;
}
