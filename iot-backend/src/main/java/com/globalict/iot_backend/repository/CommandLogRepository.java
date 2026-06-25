package com.globalict.iot_backend.repository;

import com.globalict.iot_backend.entity.CommandLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommandLogRepository extends JpaRepository<CommandLog, Long> {

    List<CommandLog> findAllByOrderBySentAtDesc();

    List<CommandLog> findAllByDeviceIdOrderBySentAtDesc(String deviceId);

    List<CommandLog> findAllBySentByOrderBySentAtDesc(String sentBy);
}
