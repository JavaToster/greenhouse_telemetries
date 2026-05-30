package com.example.greenhouse_telemetries.controllers;

import com.example.greenhouse_telemetries.DTO.cluster.ClusterTelemetryDTO;
import com.example.greenhouse_telemetries.DTO.telemetry.AddTelemetryDTO;
import com.example.greenhouse_telemetries.security.principals.DevicePrincipal;
import com.example.greenhouse_telemetries.security.principals.UserPrincipal;
import com.example.greenhouse_telemetries.services.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/telemetries")
public class TelemetryController {
    private final TelemetryService telemetryService;

    @PostMapping
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<Void> add(@AuthenticationPrincipal DevicePrincipal principal, @Valid @RequestBody AddTelemetryDTO dto) {
        telemetryService.add(dto, principal.deviceId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/clusters/{clusterId}")
    @PreAuthorize("hasRole('OWNER') and principal instanceof T(com.example.greenhouse_telemetries.security.principals.UserPrincipal)")
    public ResponseEntity<ClusterTelemetryDTO> getClusterTelemetries(
            @PathVariable("clusterId") UUID clusterId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        ClusterTelemetryDTO telemetry = telemetryService.findByCluster(clusterId, page, size);
        return ResponseEntity.ok(telemetry);
    }
}
