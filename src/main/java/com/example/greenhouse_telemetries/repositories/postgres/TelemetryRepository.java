package com.example.greenhouse_telemetries.repositories.postgres;

import com.example.greenhouse_telemetries.models.Telemetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    Page<Telemetry> findByDeviceIdIn(List<UUID> deviceIds, PageRequest pageable);
}
