package com.example.greenhouse_telemetries.DTO.telemetry;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddTelemetryDTO {
    @NotNull(message = "temperature не может быть пустым")
    @DecimalMin(value = "-50", message = "temperature должна быть не менее -50")
    @DecimalMax(value = "80", message = "temperature должна быть не более 80")
    private Double temperature;

    @NotNull(message = "air_humidity не может быть пустым")
    @DecimalMin(value = "0", message = "air_humidity должна быть от 0 до 100")
    @DecimalMax(value = "100", message = "air_humidity должна быть от 0 до 100")
    private Double airHumidity;

    @NotNull(message = "soil_humidity не может быть пустым")
    @DecimalMin(value = "0", message = "soil_humidity должна быть от 0 до 100")
    @DecimalMax(value = "100", message = "soil_humidity должна быть от 0 до 100")
    private Double soilHumidity;

    @NotNull(message = "illumination не может быть пустым")
    @DecimalMin(value = "0", message = "illumination должна быть не менее 0")
    private Double illumination;
}
