package net.liquidcars.ingestion.infra.postgresql;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.OfferEntityFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.OfferSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.service.OfferInfraSQLServiceImpl;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferInfraSQLServiceImplTest {

    @Mock
    private OfferSQLRepository sqlRepository;

    @Mock
    private OfferInfraSQLMapper mapper;

    @InjectMocks
    private OfferInfraSQLServiceImpl service;

    @Test
    void save_ShouldMapAndSaveInRepository() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferEntity entity = OfferEntityFactory.getOfferEntity();

        when(mapper.toEntity(dto)).thenReturn(entity);

        service.processOffer(dto);

        verify(mapper, times(1)).toEntity(dto);
        verify(sqlRepository, times(1)).save(entity);
    }

    @Test
    void processOffer_WhenExistsAndIsNewer_ShouldUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferEntity newEntity = OfferEntityFactory.getOfferEntity();
        OfferEntity existingEntity = OfferEntityFactory.getOfferEntity();

        newEntity.setCreatedAt(OffsetDateTime.now());
        existingEntity.setCreatedAt(OffsetDateTime.now().minusDays(1));

        String existingId = "existing-uuid-123";
        existingEntity.setId(existingId);

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(sqlRepository.findByExternalId(dto.getExternalId()))
                .thenReturn(Optional.of(existingEntity));

        service.processOffer(dto);

        verify(sqlRepository, times(1)).save(newEntity);
        assertEquals(existingId, newEntity.getId(), "The new entity ID should be updated with the existing entity's ID");
    }

    @Test
    void processOffer_WhenExistsButIsOlder_ShouldNotUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferEntity newEntity = OfferEntityFactory.getOfferEntity();
        OfferEntity existingEntity = OfferEntityFactory.getOfferEntity();

        newEntity.setCreatedAt(OffsetDateTime.now().minusDays(10));
        existingEntity.setCreatedAt(OffsetDateTime.now());

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(sqlRepository.findByExternalId(dto.getExternalId()))
                .thenReturn(Optional.of(existingEntity));

        service.processOffer(dto);

        verify(sqlRepository, never()).save(any());
    }
}
