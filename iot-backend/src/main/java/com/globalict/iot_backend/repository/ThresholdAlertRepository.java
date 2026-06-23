package com.globalict.iot_backend.repository;

import com.globalict.iot_backend.entity.ThresholdAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThresholdAlertRepository extends JpaRepository<ThresholdAlert, Long> {

    List<ThresholdAlert> findByDeviceId(Long deviceId);

    List<ThresholdAlert> findByThresholdId(Long thresholdId);

    List<ThresholdAlert> findByDeviceIdOrderByTriggeredAtDesc(Long deviceId);

    @Query("SELECT ta FROM ThresholdAlert ta WHERE ta.device.id = :deviceId AND ta.isResolved = false")
    List<ThresholdAlert> findUnresolvedByDeviceId(@Param("deviceId") Long deviceId);

    @Query("SELECT ta FROM ThresholdAlert ta WHERE ta.triggeredAt >= :startTime AND ta.triggeredAt <= :endTime")
    List<ThresholdAlert> findByTriggeredAtBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT ta FROM ThresholdAlert ta WHERE ta.isResolved = false ORDER BY ta.triggeredAt DESC")
    List<ThresholdAlert> findAllUnresolved();
}
