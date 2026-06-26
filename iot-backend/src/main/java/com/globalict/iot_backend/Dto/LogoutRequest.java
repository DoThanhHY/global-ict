package com.globalict.iot_backend.Dto;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
}
