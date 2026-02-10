package net.liquidcars.ingestion.infra.postgresql.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class OfferInfraSQLMapperTest {

    private final OfferInfraSQLMapper mapper = Mappers.getMapper(OfferInfraSQLMapper.class);

    @Test
    void toEntity_ShouldMapAllFieldsCorrectly() {
        OfferDto dto = OfferDtoFactory.getOfferDto();

        OfferEntity entity = mapper.toEntity(dto);

        assertNotNull(entity, "Entity should not be null");
        assertThat(entity.getId()).isEqualTo(dto.getId());
        assertThat(entity.getChannelReference()).isEqualTo(dto.getChannelReference());
        assertThat(entity.getVehicleInstance().getPlate()).isEqualTo(dto.getVehicleInstance().getPlate());
        assertThat(entity.getMail()).isEqualTo(dto.getMail());
        assertThat(entity.getPrice()).isEqualByComparingTo(dto.getPrice().getAmount());
    }

    @Test
    void toEntity_ShouldReturnNull_WhenDtoIsNull() {
        OfferEntity entity = mapper.toEntity(null);

        assertNull(entity, "Entity should be null when DTO is null");
    }
}
