package com.example.greenhouse_telemetries.repositories.postgres;

import com.example.greenhouse_telemetries.models.Telemetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    @Query(value = """
    SELECT t.* FROM (
        SELECT *, ROW_NUMBER() OVER (PARTITION BY device_id ORDER BY created_at DESC) as rn
        FROM telemetries
        WHERE device_id IN (:deviceIds)
    ) t
    WHERE t.rn <= :limit
    """, nativeQuery = true)
    List<Telemetry> findTopNByDeviceIds(@Param("deviceIds") List<UUID> deviceIds, @Param("limit") int limit);}
