package com.example.greenhouse_telemetries.security.principals;

import com.example.greenhouse_telemetries.util.enums.Role;

public record UserPrincipal(Long telegramId, Role role) {
}
