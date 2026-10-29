package ru.polyshelf.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OffSearchResponse(
        List<OffProductDto> products
) {}
