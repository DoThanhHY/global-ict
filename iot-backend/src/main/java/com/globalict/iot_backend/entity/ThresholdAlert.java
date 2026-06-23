package com.globalict.iot_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "threshold_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "threshold_id", nullable = false)
    private Threshold threshold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "actual_value", nullable = false)
    private Double actualValue;

    @Column(name = "alert_type", nullable = false)
    private String alertType;  // "MIN_EXCEEDED", "MAX_EXCEEDED"

    @Column(name = "threshold_value", nullable = false)
    private Double thresholdValue;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "is_resolved")
    private Boolean isResolved = false;

    @PrePersist
    protected void onCreate() {
        triggeredAt = LocalDateTime.now();
    }
}
