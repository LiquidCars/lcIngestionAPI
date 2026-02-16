package net.liquidcars.ingestion.infra.mongodb;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.DraftOfferNoSQLEntityFactory;
import net.liquidcars.ingestion.infra.mongodb.entity.DraftOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.DraftOfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.OfferInfraNoSQLServiceImpl;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.liquidcars.ingestion.factory.OfferDtoFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferInfraNoSQLServiceImplTest {

    @Mock
    private DraftOfferNoSqlRepository repository;

    @Mock
    private OfferInfraNoSQLMapper mapper;

    @InjectMocks
    private OfferInfraNoSQLServiceImpl service;

    @Test
    void save_ShouldMapAndPersistInMongo() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity entity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();

        when(mapper.toEntity(dto)).thenReturn(entity);

        service.processOffer(dto);

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).save(entity);
    }

    @Test
    void processOffer_WhenOfferExistsAndIsNewer_ShouldUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity newEntity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();
        DraftOfferNoSQLEntity existingEntity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();
        UUID existingId = UUID.randomUUID();

        existingEntity.setCreatedAt(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
        newEntity.setCreatedAt(Instant.now());
        existingEntity.setId(existingId);

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(repository.findById(dto.getId()))
                .thenReturn(Optional.of(existingEntity));

        service.processOffer(dto);

        verify(repository, times(1)).save(newEntity);
        assert(newEntity.getId().equals(existingId));
    }

    @Test
    void processOffer_WhenOfferExistsButIsOlder_ShouldNotUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity newEntity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();
        DraftOfferNoSQLEntity existingEntity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();

        // existing is artificially in the future → guarantees it is newer
        existingEntity.setCreatedAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(repository.findById(dto.getId()))
                .thenReturn(Optional.of(existingEntity));

        service.processOffer(dto);

        verify(repository, never()).save(any());
    }
}
