package net.liquidcars.ingestion.infra.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.OfferEntityFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.entity.VehicleModelEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionReportRepository;
import net.liquidcars.ingestion.infra.postgresql.repository.OfferSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.repository.VehicleModelSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.service.OfferInfraSQLServiceImpl;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferInfraSQLServiceImplTest {

    @Mock
    private OfferSQLRepository offerSqlRepository;

    @Mock
    private VehicleModelSQLRepository vehicleModelRepository;

    @Mock
    private IngestionReportRepository reportRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OfferInfraSQLMapper mapper;

    @InjectMocks
    private OfferInfraSQLServiceImpl service;

    @Test
    void save_ShouldMapAndSaveInRepository() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferEntity entity = OfferEntityFactory.getOfferEntity();

        when(mapper.toEntity(dto)).thenReturn(entity);
        when(offerSqlRepository.findById(dto.getId())).thenReturn(Optional.empty());
        when(vehicleModelRepository.findById(any()))
                .thenReturn(Optional.of(mock(VehicleModelEntity.class)));
        when(objectMapper.convertValue(any(), eq(java.util.Map.class)))
                .thenReturn(new HashMap<>());

        service.processOffer(dto);

        verify(mapper, times(1)).toEntity(dto);
        verify(offerSqlRepository, times(1)).save(entity);
    }

    @Test
    void processOffer_WhenExistsAndIsNewer_ShouldUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferEntity newEntity = OfferEntityFactory.getOfferEntity();
        OfferEntity existingEntity = OfferEntityFactory.getOfferEntity();

        newEntity.setCreatedAt(OffsetDateTime.now());
        existingEntity.setCreatedAt(OffsetDateTime.now().minusDays(1));

        UUID existingId = UUID.randomUUID();
        existingEntity.setId(existingId);

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(offerSqlRepository.findById(dto.getId()))
                .thenReturn(Optional.of(existingEntity));
        when(vehicleModelRepository.findById(any()))
                .thenReturn(Optional.of(mock(VehicleModelEntity.class)));
        when(objectMapper.convertValue(any(), eq(java.util.Map.class)))
                .thenReturn(new HashMap<>());

        service.processOffer(dto);

        verify(offerSqlRepository, times(1)).save(newEntity);
        assertEquals(existingId, newEntity.getId());
    }

    @Test
    void processOffer_WhenExistsButIsOlder_ShouldNotUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferEntity newEntity = OfferEntityFactory.getOfferEntity();
        OfferEntity existingEntity = OfferEntityFactory.getOfferEntity();

        newEntity.setCreatedAt(OffsetDateTime.now().minusDays(10));
        existingEntity.setCreatedAt(OffsetDateTime.now());

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(offerSqlRepository.findById(dto.getId()))
                .thenReturn(Optional.of(existingEntity));
        when(objectMapper.convertValue(any(), eq(java.util.Map.class)))
                .thenReturn(new HashMap<>());
        when(vehicleModelRepository.findById(any()))
                .thenReturn(Optional.of(mock(VehicleModelEntity.class)));

        service.processOffer(dto);

        verify(offerSqlRepository, never()).save(any());
    }
}
