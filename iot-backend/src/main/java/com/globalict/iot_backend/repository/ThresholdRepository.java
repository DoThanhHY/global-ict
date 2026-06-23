package com.globalict.iot_backend.repository;

import com.globalict.iot_backend.entity.Threshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThresholdRepository extends JpaRepository<Threshold, Long> {

    List<Threshold> findByDeviceId(Long deviceId);

    Optional<Threshold> findByDeviceIdAndField(Long deviceId, String field);

    List<Threshold> findByDeviceIdAndFieldIn(Long deviceId, List<String> fields);
}
