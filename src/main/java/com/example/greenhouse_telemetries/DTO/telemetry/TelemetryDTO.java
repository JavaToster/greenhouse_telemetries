package com.example.greenhouse_telemetries.DTO.telemetry;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class TelemetryDTO {
    private Long id;
    private UUID deviceId;
    private double temperature;
    private double airHumidity;
    private double soilHumidity;
    private double illumination;
    private Instant createdAt;
}
