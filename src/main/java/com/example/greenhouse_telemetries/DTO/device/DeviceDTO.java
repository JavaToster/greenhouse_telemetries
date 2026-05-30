package com.example.greenhouse_telemetries.DTO.device;

import com.example.greenhouse_telemetries.util.enums.DeviceStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class DeviceDTO {
    private UUID id;
    private DeviceStatus status;
}
