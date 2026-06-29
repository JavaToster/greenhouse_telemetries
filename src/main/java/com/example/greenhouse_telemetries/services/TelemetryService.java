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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // По умолчанию быстрая оптимизация на чтение
public class TelemetryService {
    private final TelemetryStore telemetryStore;
    private final DeviceClient deviceClient;
    private final Convertor convertor;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Telemetry add(AddTelemetryDTO dto, UUID deviceId) {
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

    public ClusterTelemetryDTO findByCluster(UUID clusterId, int limit) {
        log.info("Fetching top {} telemetry records for each device in cluster [{}]", limit, clusterId);

        log.debug("Requesting devices from inventory for cluster [{}] via Feign", clusterId);
        List<DeviceDTO> devices = deviceClient.getDevicesByCluster(clusterId);
        log.debug("Inventory returned {} devices for cluster [{}]", devices.size(), clusterId);

        if (devices.isEmpty()) {
            log.warn("Cluster [{}] has no registered devices in inventory. Returning empty telemetry.", clusterId);
            return createEmptyClusterTelemetryDTO(clusterId);
        }

        List<UUID> deviceIds = devices.stream().map(DeviceDTO::getId).toList();
        log.debug("Extracted device IDs for window-function DB query: {}", deviceIds);

        List<Telemetry> latestTelemetries = telemetryStore.findTopNByDeviceIds(deviceIds, limit);
        log.debug("DB query returned {} total telemetry records for all devices combined", latestTelemetries.size());

        Map<UUID, List<TelemetryDTO>> telemetryByDevice = latestTelemetries.stream()
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

        log.info("Successfully compiled cluster [{}] telemetry with window-function filtering.", clusterId);
        return result;
    }

    private ClusterTelemetryDTO createEmptyClusterTelemetryDTO(UUID clusterId) {
        ClusterTelemetryDTO result = new ClusterTelemetryDTO();
        result.setClusterId(clusterId);
        result.setDevices(List.of());
        return result;
    }
}