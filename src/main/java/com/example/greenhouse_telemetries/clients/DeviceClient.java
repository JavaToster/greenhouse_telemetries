package com.example.greenhouse_telemetries.clients;

import com.example.greenhouse_telemetries.DTO.device.DeviceDTO;
import com.example.greenhouse_telemetries.configurations.feign.InventoryClientErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "inventory-service",
        path = "/api/devices",
        url = "${app.clients.inventory.devices.url}",
        configuration = InventoryClientErrorDecoder.class
)
public interface DeviceClient {
    @GetMapping("/my-clusters/{clusterId}")
    List<DeviceDTO> getDevicesByCluster(@PathVariable("clusterId")UUID clusterId);

}
