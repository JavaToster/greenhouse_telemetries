package com.example.greenhouse_telemetries.security.jwt;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.greenhouse_telemetries.exceptions.auth.InvalidTokenTypeException;
import com.example.greenhouse_telemetries.security.principals.DevicePrincipal;
import com.example.greenhouse_telemetries.security.principals.UserPrincipal;
import com.example.greenhouse_telemetries.util.enums.TokenType;
import com.example.greenhouse_telemetries.util.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationProvider {
    private static final String CLUSTER_ID_CLAIM = "cluster_id";

    private final JwtUtil jwtUtil;

    public Authentication authenticate(String token) {
        DecodedJWT jwt = jwtUtil.verify(token);
        TokenType tokenType;
        try {
            tokenType = jwtUtil.getTokenType(jwt);
        } catch (RuntimeException ex) {
            log.warn("Failed to extract or parse token_type claim from JWT");
            throw new InvalidTokenTypeException("Invalid token_type claim", ex);
        }

        log.debug("Extracted token type: {} from JWT payload", tokenType);

        return switch (tokenType) {
            case USER -> authenticateUser(jwt, token);
            case DEVICE -> authenticateDevice(jwt, token);
        };
    }

    private Authentication authenticateUser(DecodedJWT jwt, String token) {
        long telegramId = Long.parseLong(jwt.getSubject());
        Role role = jwtUtil.getRole(jwt);

        log.debug("Authenticating USER principal: telegramId={}, role={}", telegramId, role);
        UserPrincipal userPrincipal = new UserPrincipal(telegramId, role);

        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                token,
                Collections.singletonList(new SimpleGrantedAuthority(role.name()))
        );
    }

    private Authentication authenticateDevice(DecodedJWT jwt, String token) {
        UUID deviceId = UUID.fromString(jwt.getSubject());
        String clusterIdRaw = jwt.getClaim(CLUSTER_ID_CLAIM).asString();

        if (clusterIdRaw == null || clusterIdRaw.isBlank()) {
            log.warn("Device authentication rejected: missing required claim '{}' for device ID [{}]", CLUSTER_ID_CLAIM, deviceId);
            throw new IllegalArgumentException("Device token has no cluster_id claim");
        }

        UUID clusterId = UUID.fromString(clusterIdRaw);
        log.debug("Authenticating DEVICE principal: deviceId=[{}], bound to cluster=[{}]", deviceId, clusterId);

        DevicePrincipal principal = new DevicePrincipal(deviceId, clusterId);
        return new UsernamePasswordAuthenticationToken(
                principal,
                token,
                List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
        );
    }
}