package ru.polyshelf.core.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DevJwtService {

    private final ObjectMapper objectMapper;

    public UUID extractUserId(String authorization) {
        String token = authorization.replaceFirst("^Bearer\\s+", "");

        try {
            String[] parts = token.split("\\.");
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode json = objectMapper.readTree(payload);
            return UUID.fromString(json.get("sub").asString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT format", e);
        }
    }
}
