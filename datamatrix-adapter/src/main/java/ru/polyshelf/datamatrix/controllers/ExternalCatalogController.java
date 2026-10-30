package ru.polyshelf.datamatrix.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.polyshelf.datamatrix.dto.DatamatrixRequest;
import ru.polyshelf.datamatrix.dto.ProductInfoResponse;
import ru.polyshelf.datamatrix.services.ExternalCatalogService;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class ExternalCatalogController {

    private final ExternalCatalogService externalCatalogService;

    @PostMapping("/datamatrix")
    public ResponseEntity<ProductInfoResponse> getInfoByDatamatrix(@Valid @RequestBody DatamatrixRequest request) {
        return ResponseEntity.ok(externalCatalogService.getCzInfo(request));
    }

}