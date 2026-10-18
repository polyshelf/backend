package ru.polyshelf.core.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.core.models.ProductEntity;
import ru.polyshelf.core.repositories.ProductRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductByCode_shouldReturnProductWhenExists() {
        String barcode = "4601234567890";

        ProductEntity expectedProduct = new ProductEntity();
        expectedProduct.setName("Молоко");

        when(productRepository.findByBarcode(barcode))
                .thenReturn(Optional.of(expectedProduct));

        ProductEntity result = productService.getProductByCode(barcode);

        assertSame(expectedProduct, result);

        verify(productRepository).findByBarcode(barcode);
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void getProductByCode_shouldThrow404WhenProductNotFound() {
        String barcode = "4601234567890";

        when(productRepository.findByBarcode(barcode))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProductByCode(barcode)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(productRepository).findByBarcode(barcode);
    }

    @Test
    void getProductByCode_shouldStripBarcode() {
        String barcode = "4601234567890";

        ProductEntity product = new ProductEntity();

        when(productRepository.findByBarcode(barcode))
                .thenReturn(Optional.of(product));

        ProductEntity result =
                productService.getProductByCode("  " + barcode + "  ");

        assertSame(product, result);

        verify(productRepository).findByBarcode(barcode);
    }

    @Test
    void checkNotExistsInCatalog_shouldAllowUnknownBarcode() {
        String barcode = "4601234567890";
        when(productRepository.existsByBarcode(barcode)).thenReturn(false);

        assertDoesNotThrow(() -> productService.checkNotExistsInCatalog(barcode));

        verify(productRepository).existsByBarcode(barcode);
    }

    @Test
    void checkNotExistsInCatalog_shouldThrow409WhenProductExists() {
        String barcode = "4601234567890";
        when(productRepository.existsByBarcode(barcode)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.checkNotExistsInCatalog(barcode)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(productRepository).existsByBarcode(barcode);
    }

    @Test
    void checkNotExistsInCatalog_shouldStripBarcode() {
        String barcode = "4601234567890";

        productService.checkNotExistsInCatalog("  " + barcode + "  ");

        verify(productRepository).existsByBarcode(barcode);
    }

}
