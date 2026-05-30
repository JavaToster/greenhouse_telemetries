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
        log.debug("Adding telemetry: temperature={}, airHumidity={}, soilHumidity={}, illumination={}",
                dto.getTemperature(), dto.getAirHumidity(), dto.getSoilHumidity(), dto.getIllumination());


        Telemetry telemetry = new Telemetry(
                deviceId,
                dto.getTemperature(),
                dto.getAirHumidity(),
                dto.getSoilHumidity(),
                dto.getIllumination()
        );

        return telemetryStore.save(telemetry);
    }

    public ClusterTelemetryDTO findByCluster(UUID clusterId, int page, int size) {
        List<DeviceDTO> devices = deviceClient.getDevicesByCluster(clusterId);

        if (devices.isEmpty()) {
            return createEmptyClusterTelemetryDTO(clusterId, page, size);
        }

        List<UUID> deviceIds = devices.stream().map(DeviceDTO::getId).toList();

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Telemetry> telemetryPage = telemetryStore.findByDeviceId(deviceIds, pageable);

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
