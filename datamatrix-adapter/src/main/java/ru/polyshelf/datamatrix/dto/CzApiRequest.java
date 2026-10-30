package ru.polyshelf.datamatrix.dto;

public record CzApiRequest(
        String code,
        String codeType
) {
    public CzApiRequest(String code) {
        this(code, "datamatrix");
    }
}