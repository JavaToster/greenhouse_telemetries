package com.example.greenhouse_telemetries.configurations.feign;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.example.greenhouse_telemetries.clients")
public class FeignConfiguration {
}
