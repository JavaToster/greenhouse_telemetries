package com.example.greenhouse_telemetries.DTO.telemetry;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DeviceTelemetryDTO {
    private UUID deviceId;
    private List<TelemetryDTO> telemetries;
}
