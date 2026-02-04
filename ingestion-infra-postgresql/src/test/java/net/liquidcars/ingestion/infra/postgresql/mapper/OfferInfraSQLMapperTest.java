package net.liquidcars.ingestion.infra.postgresql.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

public class OfferInfraSQLMapperTest {

    private final OfferInfraSQLMapper mapper = Mappers.getMapper(OfferInfraSQLMapper.class);

    @Test
    void toEntity_ShouldMapAllFieldsCorrectly() {
        OfferDto dto = OfferDtoFactory.getOfferDto();

        OfferEntity entity = mapper.toEntity(dto);

        assertNotNull(entity, "Entity should not be null");
        assertEquals(dto.getExternalId(), entity.getExternalId(), "ExternalId mapping failed");
        assertEquals(dto.getBrand(), entity.getBrand(), "Brand mapping failed");
        assertEquals(dto.getModel(), entity.getModel(), "Model mapping failed");
        assertEquals(dto.getYear(), entity.getYear(), "Year mapping failed");
        assertEquals(dto.getPrice(), entity.getPrice(), "Price mapping failed");
        assertEquals(dto.getCreatedAt(), entity.getCreatedAt(), "CreatedAt mapping failed");
        assertEquals(dto.getSource(), entity.getSource(), "Source mapping failed");

        assertEquals(dto.getVehicleType().name(), entity.getVehicleType().name(), "VehicleType enum mapping failed");
        assertEquals(dto.getStatus().name(), entity.getStatus().name(), "Status enum mapping failed");
    }

    @Test
    void toEntity_ShouldReturnNull_WhenDtoIsNull() {
        OfferEntity entity = mapper.toEntity(null);

        assertNull(entity, "Entity should be null when DTO is null");
    }

    @Test
    void toEntity_ShouldMapAllVehicleTypes() {
        for (OfferDto.VehicleTypeDto type : OfferDto.VehicleTypeDto.values()) {
            OfferDto dto = OfferDtoFactory.getOfferDto();
            dto.setVehicleType(type);

            OfferEntity entity = mapper.toEntity(dto);

            assertNotNull(entity);
            assertEquals(type.name(), entity.getVehicleType().name(),
                    "Failed mapping for VehicleType: " + type);
        }
    }

    @Test
    void toEntity_ShouldMapAllOfferStatuses() {
        for (OfferDto.OfferStatusDto status : OfferDto.OfferStatusDto.values()) {
            OfferDto dto = OfferDtoFactory.getOfferDto();
            dto.setStatus(status);

            OfferEntity entity = mapper.toEntity(dto);

            assertNotNull(entity);
            assertEquals(status.name(), entity.getStatus().name(),
                    "Failed mapping for OfferStatus: " + status);
        }
    }

    @Test
    void toEntity_ShouldHandleNullEnums() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        dto.setVehicleType(null);
        dto.setStatus(null);

        OfferEntity entity = mapper.toEntity(dto);

        assertNotNull(entity);
        assertNull(entity.getVehicleType(), "VehicleType should be null");
        assertNull(entity.getStatus(), "Status should be null");
    }
}
