package com.example.greenhouse_telemetries.controllers;

import com.example.greenhouse_telemetries.DTO.cluster.ClusterTelemetryDTO;
import com.example.greenhouse_telemetries.DTO.telemetry.AddTelemetryDTO;
import com.example.greenhouse_telemetries.security.principals.DevicePrincipal;
import com.example.greenhouse_telemetries.security.principals.UserPrincipal;
import com.example.greenhouse_telemetries.services.TelemetryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/telemetries")
@Validated
public class TelemetryController {
    private final TelemetryService telemetryService;

    @PostMapping
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<Void> add(@AuthenticationPrincipal DevicePrincipal principal, @Valid @RequestBody AddTelemetryDTO dto) {
        telemetryService.add(dto, principal.deviceId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/clusters/{clusterId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') and principal instanceof T(com.example.greenhouse_telemetries.security.principals.UserPrincipal)")
    public ResponseEntity<ClusterTelemetryDTO> getClusterTelemetries(
            @PathVariable("clusterId") UUID clusterId,
            @RequestParam(name = "limit", defaultValue = "50")
            @Max(value = 200, message = "size must not exceed 200")
            @Min(value = 1, message = "size must be greater than 0") int limit
    ) {
        ClusterTelemetryDTO telemetry = telemetryService.findByCluster(clusterId, limit);
        return ResponseEntity.ok(telemetry);
    }
}
