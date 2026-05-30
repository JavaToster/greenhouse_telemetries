package com.example.greenhouse_telemetries.store;

import com.example.greenhouse_telemetries.models.Telemetry;
import com.example.greenhouse_telemetries.repositories.postgres.TelemetryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TelemetryStore implements GenericStore<Telemetry, Long> {
    private final TelemetryRepository telemetryRepository;

    @Override
    public Telemetry findById(Long id) {
        return telemetryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Telemetry with id " + id + " not found"));
    }

    @Override
    public Telemetry save(Telemetry telemetry) {
        return telemetryRepository.save(telemetry);
    }

    public Page<Telemetry> findByDeviceId(List<UUID> deviceIds, PageRequest pageable) {
        return telemetryRepository.findByDeviceIdIn(deviceIds,pageable);
    }
}
