package com.example.greenhouse_telemetries.DTO.cluster;

import com.example.greenhouse_telemetries.DTO.telemetry.DeviceTelemetryDTO;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ClusterTelemetryDTO {
    private UUID clusterId;
    private List<DeviceTelemetryDTO> devices;
}
