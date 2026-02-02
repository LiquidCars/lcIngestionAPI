package net.liquidcars.ingestion.infra.postgresql;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.OfferSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.service.OfferInfraSQLServiceImpl;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        OfferDto dto = new OfferDto();
        OfferEntity entity = new OfferEntity();

        when(mapper.toEntity(dto)).thenReturn(entity);

        service.processOffer(dto);

        verify(mapper, times(1)).toEntity(dto);
        verify(sqlRepository, times(1)).save(entity);
    }
}
