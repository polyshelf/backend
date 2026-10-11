package ru.polyshelf.gateway.scan.model;

import java.util.Map;

public record PreparedRequest(String url, Map<String, String> headers) {
}
