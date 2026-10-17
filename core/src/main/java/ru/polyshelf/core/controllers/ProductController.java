package ru.polyshelf.core.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.polyshelf.core.dto.ProductResponse;
import ru.polyshelf.core.dto.ProductWithBarcodeCandidateRequest;
import ru.polyshelf.core.dto.ProductWithBarcodeRequest;
import ru.polyshelf.core.dto.ProductWithoutCodeRequest;
import ru.polyshelf.core.security.UserPrincipal;
import ru.polyshelf.core.services.InventoryService;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final InventoryService inventoryService;

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/from-barcode")
    public ResponseEntity<ProductResponse> addWithBarcode(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ProductWithBarcodeRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(inventoryService.addWithCode(user.userId(), request));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/from-barcode-candidate")
    public ResponseEntity<ProductResponse> addWithBarcodeCandidate(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ProductWithBarcodeCandidateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(inventoryService.addWithCodeCandidate(user.userId(), request));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ProductResponse> addWithoutCode(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ProductWithoutCodeRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(inventoryService.addWithoutCode(user.userId(), request));
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteFromInventory(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID itemId
    ) {
        inventoryService.delete(user.userId(), itemId);
        return ResponseEntity.noContent().build();
    }

}
