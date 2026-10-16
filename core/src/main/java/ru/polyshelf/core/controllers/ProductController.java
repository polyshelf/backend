package ru.polyshelf.core.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.services.InventoryService;
import ru.polyshelf.core.utils.DevJwtService;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final DevJwtService jwtService;
    private final InventoryService inventoryService;

    //@SecurityRequirement(name = "bearerAuth")
    //@PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ProductResponse> addWithoutCode(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ProductWithoutCodeRequest request
    ) {
        UUID userId = jwtService.extractUserId(authorization);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(inventoryService.addWithoutCode(userId, request));
    }

}
