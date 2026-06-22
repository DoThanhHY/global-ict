package com.globalict.iot_backend.Dto;

import lombok.Data;

@Data
public class UpdateDeviceRequest {
    private String name;
    private String location;
}