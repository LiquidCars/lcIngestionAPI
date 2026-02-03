package net.liquidcars.ingestion.infra.mongodb;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferNoSQLEntityFactory;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.OfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.OfferInfraNoSQLServiceImpl;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.liquidcars.ingestion.factory.OfferDtoFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferInfraNoSQLServiceImplTest {

    @Mock
    private OfferNoSqlRepository repository;

    @Mock
    private OfferInfraNoSQLMapper mapper;

    @InjectMocks
    private OfferInfraNoSQLServiceImpl service;

    @Test
    void save_ShouldMapAndPersistInMongo() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferNoSQLEntity entity = OfferNoSQLEntityFactory.getOfferNoSQLEntity();

        when(mapper.toEntity(dto)).thenReturn(entity);

        service.processOffer(dto);

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).save(entity);
    }

    @Test
    void processOffer_WhenOfferExistsAndIsNewer_ShouldUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferNoSQLEntity newEntity = OfferNoSQLEntityFactory.getOfferNoSQLEntity();
        OfferNoSQLEntity existingEntity = OfferNoSQLEntityFactory.getOfferNoSQLEntity();

        // Instant usa ChronoUnit para cálculos temporales
        existingEntity.setCreatedAt(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
        newEntity.setCreatedAt(Instant.now());
        existingEntity.setId("existing-id");

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(repository.findByExternalId(dto.getExternalId()))
                .thenReturn(Optional.of(existingEntity));

        service.processOffer(dto);

        verify(repository, times(1)).save(newEntity);
        assert(newEntity.getId().equals("existing-id"));
    }

    @Test
    void processOffer_WhenOfferExistsButIsOlder_ShouldNotUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferNoSQLEntity newEntity = OfferNoSQLEntityFactory.getOfferNoSQLEntity();
        OfferNoSQLEntity existingEntity = OfferNoSQLEntityFactory.getOfferNoSQLEntity();

        existingEntity.setCreatedAt(Instant.now());
        newEntity.setCreatedAt(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(repository.findByExternalId(dto.getExternalId()))
                .thenReturn(Optional.of(existingEntity));

        service.processOffer(dto);

        verify(repository, never()).save(any(OfferNoSQLEntity.class));
    }
}
