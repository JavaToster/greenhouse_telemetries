package com.example.greenhouse_telemetries.util;

import com.example.greenhouse_telemetries.DTO.telemetry.TelemetryDTO;
import com.example.greenhouse_telemetries.models.Telemetry;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Convertor {
    private final ModelMapper modelMapper;

    public TelemetryDTO convertToTelemetryDTO(Telemetry telemetry){
        return modelMapper.map(telemetry, TelemetryDTO.class);
    }
}
