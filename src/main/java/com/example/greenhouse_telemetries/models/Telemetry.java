package com.example.greenhouse_telemetries.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "telemetries")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Telemetry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "temperature", nullable = false)
    private double temperature;

    @Column(name = "air_humidity", nullable = false)
    private double airHumidity;

    @Column(name = "soil_humidity", nullable = false)
    private double soilHumidity;

    @Column(name = "illumination", nullable = false)
    private double illumination;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Telemetry(UUID deviceId, double temperature, double airHumidity, double soilHumidity, double illumination) {
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.airHumidity = airHumidity;
        this.soilHumidity = soilHumidity;
        this.illumination = illumination;
        this.createdAt = Instant.now();
    }
}
