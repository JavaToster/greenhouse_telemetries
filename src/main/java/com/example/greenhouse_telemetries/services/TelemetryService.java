package com.example.greenhouse_telemetries.services;

import com.example.greenhouse_telemetries.DTO.cluster.ClusterTelemetryDTO;
import com.example.greenhouse_telemetries.DTO.telemetry.AddTelemetryDTO;
import com.example.greenhouse_telemetries.DTO.device.DeviceDTO;
import com.example.greenhouse_telemetries.DTO.telemetry.DeviceTelemetryDTO;
import com.example.greenhouse_telemetries.DTO.telemetry.TelemetryDTO;
import com.example.greenhouse_telemetries.clients.DeviceClient;
import com.example.greenhouse_telemetries.models.Telemetry;
import com.example.greenhouse_telemetries.store.TelemetryStore;
import com.example.greenhouse_telemetries.util.Convertor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelemetryService {
    private final TelemetryStore telemetryStore;
    private final DeviceClient deviceClient;
    private final Convertor convertor;

    @Transactional
    public Telemetry add(AddTelemetryDTO dto, UUID deviceId) {
        // Поднимаем уровень до INFO, так как сохранение метрики — важное бизнес-событие
        log.info("Saving new telemetry for device [{}]: temp={}, airHum={}, soilHum={}, lux={}",
                deviceId, dto.getTemperature(), dto.getAirHumidity(), dto.getSoilHumidity(), dto.getIllumination());

        Telemetry telemetry = new Telemetry(
                deviceId,
                dto.getTemperature(),
                dto.getAirHumidity(),
                dto.getSoilHumidity(),
                dto.getIllumination()
        );

        Telemetry saved = telemetryStore.save(telemetry);
        log.debug("Telemetry successfully saved with database id: {}", saved.getId());
        return saved;
    }

    public ClusterTelemetryDTO findByCluster(UUID clusterId, int page, int size) {
        log.info("Fetching telemetry for cluster [{}] (page: {}, size: {})", clusterId, page, size);

        // Логируем запрос во внешний микросервис инвентаря
        log.debug("Requesting devices from inventory for cluster [{}] via Feign", clusterId);
        List<DeviceDTO> devices = deviceClient.getDevicesByCluster(clusterId);
        log.debug("Inventory returned {} devices for cluster [{}]", devices.size(), clusterId);

        if (devices.isEmpty()) {
            log.warn("Cluster [{}] has no registered devices in inventory. Returning empty telemetry.", clusterId);
            return createEmptyClusterTelemetryDTO(clusterId, page, size);
        }

        List<UUID> deviceIds = devices.stream().map(DeviceDTO::getId).toList();
        log.debug("Extracted device IDs for DB query: {}", deviceIds);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Telemetry> telemetryPage = telemetryStore.findByDeviceId(deviceIds, pageable);
        log.debug("DB query found {} telemetry records across all cluster devices", telemetryPage.getNumberOfElements());

        Map<UUID, List<TelemetryDTO>> telemetryByDevice = telemetryPage.getContent().stream()
                .collect(Collectors.groupingBy(
                        Telemetry::getDeviceId,
                        Collectors.mapping(convertor::convertToTelemetryDTO, Collectors.toList())
                ));

        List<DeviceTelemetryDTO> deviceTelemetry = devices.stream()
                .map(device -> {
                    DeviceTelemetryDTO dto = new DeviceTelemetryDTO();
                    dto.setDeviceId(device.getId());
                    dto.setTelemetries(telemetryByDevice.getOrDefault(device.getId(), List.of()));
                    return dto;
                })
                .toList();

        ClusterTelemetryDTO result = new ClusterTelemetryDTO();
        result.setClusterId(clusterId);
        result.setDevices(deviceTelemetry);
        result.setPage(telemetryPage.getNumber());
        result.setSize(telemetryPage.getSize());
        result.setTotalElements(telemetryPage.getTotalElements());
        result.setTotalPages(telemetryPage.getTotalPages());

        log.info("Successfully compiled cluster [{}] telemetry. Total pages: {}, total elements: {}",
                clusterId, result.getTotalPages(), result.getTotalElements());

        return result;
    }

    private ClusterTelemetryDTO createEmptyClusterTelemetryDTO(UUID clusterId, int page, int size) {
        ClusterTelemetryDTO result = new ClusterTelemetryDTO();
        result.setClusterId(clusterId);
        result.setDevices(List.of());
        result.setPage(page);
        result.setSize(size);
        result.setTotalElements(0);
        result.setTotalPages(0);
        return result;
    }
}