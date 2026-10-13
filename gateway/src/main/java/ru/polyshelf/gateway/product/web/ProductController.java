package ru.polyshelf.gateway.product.web;

import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.polyshelf.gateway.product.model.ProductNotFoundResponse;
import ru.polyshelf.gateway.product.model.ProductSuccessResponse;
import ru.polyshelf.gateway.product.service.ProductService;
import ru.polyshelf.gateway.security.AuthenticatedUser;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/from-datamatrix")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSuccessResponse fromDataMatrix(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody ProductDataMatrixRequest request) {
        return productService.addFromDataMatrix(AuthenticatedUser.from(jwt).id(), request.code().trim());
    }

    @PostMapping("/from-barcode")
    public ResponseEntity<Object> fromBarcode(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody ProductBarcodeRequest request) {
        Optional<ProductSuccessResponse> product =
                productService.addFromBarcode(AuthenticatedUser.from(jwt).id(), request);
        if (product.isPresent()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(product.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ProductNotFoundResponse.of(request.code().trim()));
    }

    @PostMapping("/from-barcode-candidate")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductSuccessResponse fromBarcodeCandidate(@AuthenticationPrincipal Jwt jwt,
                                                       @Valid @RequestBody ProductBarcodeCandidateRequest request) {
        return productService.addFromBarcodeCandidate(AuthenticatedUser.from(jwt).id(), request);
    }
}
