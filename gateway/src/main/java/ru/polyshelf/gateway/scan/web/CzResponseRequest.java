package ru.polyshelf.gateway.scan.web;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record CzResponseRequest(@NotEmpty Map<String, Object> payload) {
}
