package ru.polyshelf.gateway.product.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductDataMatrixRequest(@NotBlank @Size(max = 512) String code) {
}
