package ru.polyshelf.gateway.scan.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScanRequest(@NotBlank @Size(max = 512) String code) {
}
