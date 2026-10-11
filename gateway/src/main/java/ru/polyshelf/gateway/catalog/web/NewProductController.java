package ru.polyshelf.gateway.catalog.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.polyshelf.gateway.catalog.service.CatalogService;
import ru.polyshelf.gateway.security.AuthenticatedUser;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NewProductController {

    private final CatalogService catalogService;

    @PostMapping("/new-product")
    @ResponseStatus(HttpStatus.CREATED)
    public void createProduct(@AuthenticationPrincipal Jwt jwt,
                              @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                              @Valid @RequestBody NewProductRequest request) {
        catalogService.registerNewProduct(AuthenticatedUser.from(jwt).id(), request, idempotencyKey);
    }
}
