package com.example.greenhouse_telemetries.configurations.feign;

import com.example.greenhouse_telemetries.DTO.error.ErrorResponseDTO;
import feign.Response;
import feign.codec.ErrorDecoder;
import jakarta.persistence.EntityNotFoundException;
import org.apache.coyote.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class InventoryClientErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorDecoder errorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String defaultMessage = "Something went wrong in external service";
        if (response.body() != null) {
            try (InputStream inputStream = response.body().asInputStream()) {
                ErrorResponseDTO errorResponseDTO = objectMapper.readValue(inputStream, ErrorResponseDTO.class);
                if (errorResponseDTO.getMessage() != null) {
                    defaultMessage = errorResponseDTO.getMessage();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return switch (response.status()) {
            case 404 -> new EntityNotFoundException(defaultMessage);
            case 400 -> new BadRequestException(defaultMessage);
            case 403 -> new AccessDeniedException(defaultMessage);
            default -> errorDecoder.decode(methodKey, response);
        };
    }
}
