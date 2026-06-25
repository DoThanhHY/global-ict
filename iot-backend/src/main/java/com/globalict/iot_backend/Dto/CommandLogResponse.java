package com.globalict.iot_backend.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommandLogResponse {

    private Long id;
    private String deviceId;
    private String action;
    private String sentBy;
    private LocalDateTime sentAt;
    private String status;
    private String errorMessage;
}
