package net.liquidcars.ingestion.infra.mongodb.mapper;

import net.liquidcars.ingestion.domain.model.*;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.mongodb.entity.DraftOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.entity.KeyValueNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.entity.TinyLocatorNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.entity.VehicleOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


public class OfferInfraNoSQLMapperTest {

    private final OfferInfraNoSQLMapper mapper = new OfferInfraNoSQLMapperImpl();

    @Nested
    @DisplayName("Tests de mapeo de DTO a Entidad")
    class ToEntityTests {

        @Test
        @DisplayName("toEntity: Debe mapear OfferDto a DraftOfferNoSQLEntity correctamente")
        void toEntity_ShouldMapCorrectively() {
            // GIVEN
            UUID id = UUID.randomUUID();
            ExternalIdInfoDto externalIds = ExternalIdInfoDto.builder()
                    .ownerReference("OWNER-1")
                    .dealerReference("DEALER-1")
                    .channelReference("CH-1")
                    .build();

            OfferDto dto = OfferDto.builder()
                    .id(id)
                    .externalIdInfo(externalIds)
                    .build();

            // WHEN
            DraftOfferNoSQLEntity entity = mapper.toEntity(dto);

            // THEN
            assertThat(entity).isNotNull();
            assertThat(entity.getId()).isEqualTo(id);
            assertThat(entity.getOwnerReference()).isEqualTo("OWNER-1");
            assertThat(entity.getDealerReference()).isEqualTo("DEALER-1");
            assertThat(entity.getChannelReference()).isEqualTo("CH-1");
            assertThat(entity.getLastUpdated()).isNotNull(); // java(System.currentTimeMillis())
        }
    }

    @Nested
    @DisplayName("Tests de mapeo de Entidad a DTO")
    class ToDtoTests {

        @Test
        @DisplayName("toVehicleOfferDto: Debe reconstruir el ExternalIdDto desde los campos planos de la entidad")
        void toDto_ShouldMapFlattenedFieldsToExternalIdInfo() {
            // GIVEN
            VehicleOfferNoSQLEntity entity = new VehicleOfferNoSQLEntity();
            entity.setOwnerReference("O1");
            entity.setDealerReference("D1");
            entity.setChannelReference("C1");

            // WHEN
            OfferDto dto = mapper.toVehicleOfferDto(entity);

            // THEN
            assertThat(dto.getExternalIdInfo()).isNotNull();
            assertThat(dto.getExternalIdInfo().getOwnerReference()).isEqualTo("O1");
            assertThat(dto.getExternalIdInfo().getDealerReference()).isEqualTo("D1");
            assertThat(dto.getExternalIdInfo().getChannelReference()).isEqualTo("C1");
        }
    }

    @Nested
    @DisplayName("Tests de métodos de ayuda (Helper Methods)")
    class HelperMethodsTests {

        @Test
        @DisplayName("map (Date): OffsetDateTime a Instant y viceversa")
        void dateMapping_ShouldWorkInBothDirections() {
            OffsetDateTime odt = OffsetDateTime.of(2024, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);

            Instant instant = mapper.map(odt);
            OffsetDateTime back = mapper.map(instant);

            assertThat(instant).isEqualTo(odt.toInstant());
            assertThat(back).isEqualTo(odt);
        }

        @Test
        @DisplayName("map (KeyValue): KeyValueDto a KeyValueNoSQLEntity con conversión a String")
        void keyValueMapping_ShouldConvertToStrings() {
            // Test con tipos Integer para verificar el .toString() de la implementación
            KeyValueDto<Integer, Integer> kvDto = new KeyValueDto<>(100, 200);

            KeyValueNoSQLEntity entity = mapper.map(kvDto);

            assertThat(entity.getKey()).isEqualTo("100");
            assertThat(entity.getValue()).isEqualTo("200");
        }

        @Test
        @DisplayName("map (UUID): UUID a String y viceversa")
        void uuidMapping_ShouldHandleNullsAndStrings() {
            UUID original = UUID.randomUUID();

            String stringUuid = mapper.map(original);
            UUID back = mapper.mapToUuid(stringUuid);

            assertThat(stringUuid).isEqualTo(original.toString());
            assertThat(back).isEqualTo(original);
            assertThat(mapper.map((UUID) null)).isNull();
            assertThat(mapper.mapToUuid(null)).isNull();
        }
    }

    @Test
    @DisplayName("toVehicleOfferNoSQLEntity: Debe copiar Draft a Producción NoSQL")
    void toVehicleOffer_ShouldCopyFields() {
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());
        draft.setOwnerReference("REF");
        List<TinyLocatorNoSQLEntity> tinyLocators = TestDataFactory.createTinyLocatorNoSQLEntityList(3);
        VehicleOfferNoSQLEntity vehicle = mapper.toVehicleOfferNoSQLEntity(draft, tinyLocators);

        assertThat(vehicle.getId()).isEqualTo(draft.getId());
        assertThat(vehicle.getOwnerReference()).isEqualTo(draft.getOwnerReference());
    }

    @Test
    @DisplayName("Debe mapear el árbol completo de OfferDto para cubrir métodos internos")
    void fullMappingCoverageTest() {
        DraftOfferNoSQLEntity draft = TestDataFactory.createDraftOfferNoSQLEntity();
        List<TinyLocatorNoSQLEntity> tinyLocators = TestDataFactory.createTinyLocatorNoSQLEntityList(3);

        VehicleOfferNoSQLEntity entity = mapper.toVehicleOfferNoSQLEntity(draft, tinyLocators);

        entity.setPrivateOwnerRegisteredUserId(UUID.randomUUID().toString());
        entity.setJsonCarOfferId(UUID.randomUUID().toString());

        VehicleOfferDto backDto = mapper.toVehicleOfferDto(entity);

        assertThat(backDto).isNotNull();
    }

}
