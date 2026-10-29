package ru.polyshelf.core.dto;

import com.jerolba.carpet.annotation.Alias;

import java.util.List;

public record OffProductDto(
        String code,
        @Alias("product_name") List<LocalizedText> productName
) {
    public record LocalizedText(String lang, String text) {}
}
