package ru.polyshelf.core.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.polyshelf.core.models.ProductCandidateEntity;
import ru.polyshelf.core.repositories.ProductCandidateRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCandidateServiceTest {

    @Mock
    private ProductCandidateRepository productCandidateRepository;

    @InjectMocks
    private ProductCandidateService productCandidateService;

    private static final UUID USER_ID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test
    void addByUser_shouldSavePendingCandidate() {
        productCandidateService.addByUser(USER_ID, "4601234567890", "Молоко");

        ArgumentCaptor<ProductCandidateEntity> captor =
                ArgumentCaptor.forClass(ProductCandidateEntity.class);
        verify(productCandidateRepository).save(captor.capture());

        ProductCandidateEntity candidate = captor.getValue();
        assertAll(
                () -> assertEquals("4601234567890", candidate.getBarcode()),
                () -> assertEquals("Молоко", candidate.getName()),
                () -> assertEquals(USER_ID, candidate.getUserId()),
                () -> assertEquals(ProductCandidateEntity.Status.PENDING, candidate.getStatus()),
                () -> assertNull(candidate.getId()),
                () -> assertNull(candidate.getCreatedAt())
        );
        verifyNoMoreInteractions(productCandidateRepository);
    }

    @Test
    void addByUser_shouldStripBarcodeAndName() {
        productCandidateService.addByUser(USER_ID, "  4601234567890  ", "  Молоко  ");

        ArgumentCaptor<ProductCandidateEntity> captor =
                ArgumentCaptor.forClass(ProductCandidateEntity.class);
        verify(productCandidateRepository).save(captor.capture());
        assertEquals("4601234567890", captor.getValue().getBarcode());
        assertEquals("Молоко", captor.getValue().getName());
    }

    @Test
    void addByUser_shouldPropagateRepositoryException() {
        when(productCandidateRepository.save(any(ProductCandidateEntity.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> productCandidateService.addByUser(USER_ID, "4601234567890", "Молоко")
        );

        assertEquals("Database unavailable", exception.getMessage());
        verify(productCandidateRepository).save(any(ProductCandidateEntity.class));
    }

}
