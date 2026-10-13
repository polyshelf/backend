package ru.polyshelf.gateway.auth.service;

public record AccessToken(String value, long expiresInSeconds) {
}
