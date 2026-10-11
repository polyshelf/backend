package ru.polyshelf.gateway.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public record RsaKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
}
