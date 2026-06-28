package com.globalict.iot_backend.repository;

import com.globalict.iot_backend.entity.SensorData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    @Query("SELECT s FROM SensorData s WHERE s.device.deviceId = :deviceId ORDER BY s.recordedAt DESC")
    List<SensorData> findLatestByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    @Query("SELECT s FROM SensorData s WHERE s.device.deviceId = :deviceId AND s.recordedAt BETWEEN :from AND :to ORDER BY s.recordedAt DESC")
    List<SensorData> findByDeviceIdAndRecordedAtBetween(
            @Param("deviceId") String deviceId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
        SELECT new map(
            COUNT(DISTINCT s.device.id) as totalDevices,
            COUNT(s.id) as totalRecords
        )
        FROM SensorData s
        WHERE s.recordedAt >= :since
        """)
    Map<String, Object> getDashboardStats(@Param("since") LocalDateTime since);
}