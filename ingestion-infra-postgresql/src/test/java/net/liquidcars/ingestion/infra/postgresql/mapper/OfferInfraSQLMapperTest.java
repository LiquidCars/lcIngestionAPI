package net.liquidcars.ingestion.infra.postgresql.mapper;

import net.liquidcars.ingestion.domain.model.*;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.*;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferInfraSQLMapperTest {

    private OfferInfraSQLMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OfferInfraSQLMapperImpl();
    }

    @Test
    @DisplayName("Should map CarInstanceEquipmentDto to Entity and handle list")
    void testCarInstanceEquipmentMapping() {
        CarInstanceEquipmentDto dto = TestDataFactory.createCarInstanceEquipmentDtoWithData("test",
                TestDataFactory.createMoneyDtoFull(BigDecimal.valueOf(10.5), "EUR"));

        CarInstanceEquipmentEntity entity = mapper.toCarInstanceEquipmentEntity(dto);

        assertThat(entity.getId()).isEqualTo(dto.getId());
        assertThat(entity.getPrice()).isEqualTo(BigDecimal.valueOf(10.5));

        List<CarInstanceEquipmentEntity> list = mapper.toCarInstanceEquipmentEntityList(List.of(dto));
        assertThat(list).hasSize(1);

        assertThat(mapper.toCarInstanceEquipmentEntity(null)).isNull();
        assertThat(mapper.toCarInstanceEquipmentEntityList(null)).isNull();
    }

    @Test
    @DisplayName("Should map and update ParticipantAddress")
    void testAddressMapping() {
        PostalAddressDto postalAddressDto = TestDataFactory.createPostalAddressDtoWithData("41000", "Sevilla");
        ParticipantAddressDto dto = TestDataFactory.createParticipantAddressDtoWithData(postalAddressDto, AddressTypeDto.B_PICKUP);

        ParticipantAddressEntity entity = mapper.toParticipantAddressEntity(dto);
        assertThat(entity.getAddressType().getId()).isEqualTo("B_PICKUP");
        assertThat(entity.getPostalCode()).isEqualTo("41000");
        assertThat(entity.getCity()).isEqualTo("Sevilla");

        ParticipantAddressEntity existingEntity = new ParticipantAddressEntity();
        mapper.updateAddressFromDto(dto, existingEntity);
        assertThat(existingEntity.getCity()).isEqualTo("Sevilla");

        assertThat(mapper.toParticipantAddressEntity(null)).isNull();
        assertThat(mapper.addressTypeReference(null)).isNull();
    }

    @Test
    @DisplayName("Should map OfferDto to Entity with complex fields")
    void testOfferToEntity() {
        UUID invId = UUID.randomUUID();
        BigDecimal priceValue = BigDecimal.valueOf(10.5);

        OfferDto dto = OfferDto.builder()
                .price(TestDataFactory.createMoneyDtoFull(priceValue, "EUR"))
                .lastUpdated(1708329600L)
                .inventoryId(invId)
                .externalIdInfo(TestDataFactory.createExternalIdInfoFull("43343", "432", "654"))
                .build();

        OfferEntity entity = mapper.toEntity(dto);

        assertThat(entity.getPrice()).isEqualTo(priceValue);
        assertThat(entity.getCurrency().getId()).isEqualTo("EUR");
        assertThat(entity.getInventoryIds()).containsExactly(invId);

        OfferEntity existingOffer = new OfferEntity();
        mapper.updateEntityFromDto(dto, existingOffer);
        assertThat(existingOffer.getPrice()).isEqualTo(priceValue);

        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.uuidToSet(null)).isNull();
    }

    @Test
    @DisplayName("Should cover edge cases in named methods")
    void testNamedMethodsEdgeCases() {
        VehicleModelDto modelZero = new VehicleModelDto();
        modelZero.setId(0);
        assertThat(mapper.vehicleModelReference(modelZero)).isNull();

        KeyValueDto kvNull = new KeyValueDto(null, "label");
        assertThat(mapper.colorReference(kvNull)).isNull();

        assertThat(mapper.uuidToSet(null)).isNull();
    }

    @Test
    @DisplayName("Should map VehicleInstance and VehicleModel")
    void testVehicleMapping() {
        VehicleInstanceDto instanceDto = TestDataFactory.createVehicleInstanceWithChassisNumber("123");
        instanceDto.setState(new KeyValueDto("USED", "Used"));

        VehicleInstanceEntity instanceEntity = mapper.toVehicleInstanceEntity(instanceDto);
        assertThat(instanceEntity.getChassisNumber()).isEqualTo("123");
        assertThat(instanceEntity.getState().getId()).isEqualTo("USED");

        VehicleModelDto modelDto = TestDataFactory.createVehicleModelDto();
        modelDto.setFuelType(new KeyValueDto("GASOLINE", "Gasoline"));

        VehicleModelEntity modelEntity = mapper.toVehicleModelEntity(modelDto);
        assertThat(modelEntity.getFuelType()).isNotNull();
        assertThat(modelEntity.getFuelType().getId()).isEqualTo("GASOLINE");

        assertThat(mapper.toVehicleInstanceEntity(null)).isNull();
        assertThat(mapper.toVehicleModelEntity(null)).isNull();
    }

    @Test
    @DisplayName("Should handle null nested objects in OfferDto mappings")
    void testOfferDtoNestedNulls() {
        OfferDto dtoWithNulls = OfferDto.builder()
                .priceNew(null)
                .financedPrice(null)
                .build();

        OfferEntity entity = mapper.toEntity(dtoWithNulls);

        assertThat(entity.getPriceNew()).isNull();
        assertThat(entity.getFinancedPrice()).isNull();

        OfferEntity existing = new OfferEntity();
        mapper.updateEntityFromDto(dtoWithNulls, existing);
        assertThat(existing.getPriceNew()).isNull();
    }

    @Test
    @DisplayName("Should cover carOfferSellerTypeEnumDtoToCarOfferSellerTypeEnumEntity null branch")
    void testSellerTypeNull() {
        OfferDto dto = TestDataFactory.createOfferDtoWithNullPrices();

        OfferEntity entity = mapper.toEntity(dto);

        assertThat(entity.getSellerType()).isNull();
    }

    @Test
    @DisplayName("Should cover dtoFinancedPriceAmount and dtoPriceNewAmount null branches")
    void testOfferPriceAmountsNull() {
        OfferDto dto = TestDataFactory.createOfferDtoWithNullPrices();

        OfferEntity entity = mapper.toEntity(dto);

        assertThat(entity.getPriceNew()).isNull();
        assertThat(entity.getFinancedPrice()).isNull();
    }

    @Test
    @DisplayName("Should cover Latitude and Longitude null branches in Address")
    void testAddressGpsNull() {
        ParticipantAddressDto dto = TestDataFactory.createParticipantAddressDtoWithNullGps();

        ParticipantAddressEntity entity = mapper.toParticipantAddressEntity(dto);

        assertThat(entity.getLatitude()).isZero();
        assertThat(entity.getLongitude()).isZero();
    }

    @Test
    @DisplayName("Case 1: Valid Enum value (Covers the mapping logic)")
    void testSellerTypeValid() {
        OfferDto dto = TestDataFactory.createOfferDtoWithSellerType(CarOfferSellerTypeEnumDto.usedCar_PrivateSeller);

        OfferEntity entity = mapper.toEntity(dto);

        assertThat(entity.getSellerType()).isNotNull();
        assertThat(entity.getSellerType().name()).isEqualTo("usedCar_PrivateSeller");
    }

    @Test
    @DisplayName("Case 2: Null Enum value (Covers the 'if field == null' branch)")
    void testSellerTypeNullField() {
        OfferDto dto = TestDataFactory.createOfferDtoWithSellerType(null);

        OfferEntity entity = mapper.toEntity(dto);

        assertThat(entity.getSellerType()).isNull();
    }

    @Test
    @DisplayName("Case 3: Null Root DTO (Covers the 'if dto == null' branch of the generated method)")
    void testSellerTypeNullDto() {
        OfferEntity entity = mapper.toEntity(null);

        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Full coverage for KeyValue reference methods")
    void testKeyValueReferencesCoverage() {
        assertThat(mapper.colorReference(null)).isNull();
        assertThat(mapper.fuelTypeReference(null)).isNull();

        KeyValueDto<String, String> dtoWithNullKey = TestDataFactory.createKeyValueDtoWithNullKey();

        assertThat(mapper.colorReference(dtoWithNullKey)).isNull();
        assertThat(mapper.fuelTypeReference(dtoWithNullKey)).isNull();
        assertThat(mapper.bodyTypeReference(dtoWithNullKey)).isNull();

        KeyValueDto<String, String> validDto = new KeyValueDto<>("RED", "Rojo");
        assertThat(mapper.colorReference(validDto).getId()).isEqualTo("RED");
    }

    @Test
    @DisplayName("Should cover 100% of all KeyValue reference methods")
    void testAllKeyValueReferencesFullCoverage() {
        assertThat(mapper.equipmentTypeReference(null)).isNull();
        assertThat(mapper.equipmentCategoryReference(null)).isNull();
        assertThat(mapper.equipmentReference(null)).isNull();
        assertThat(mapper.environmentalBadgeReference(null)).isNull();
        assertThat(mapper.drivetrainTypeReference(null)).isNull();
        assertThat(mapper.changeTypeReference(null)).isNull();
        assertThat(mapper.bodyTypeReference(null)).isNull();
        assertThat(mapper.colorReference(null)).isNull();
        assertThat(mapper.fuelTypeReference(null)).isNull();

        KeyValueDto dtoWithNullKey = TestDataFactory.createKeyValueDtoWithNullKey();

        assertThat(mapper.equipmentTypeReference(dtoWithNullKey)).isNull();
        assertThat(mapper.equipmentCategoryReference(dtoWithNullKey)).isNull();
        assertThat(mapper.equipmentReference(dtoWithNullKey)).isNull();
        assertThat(mapper.environmentalBadgeReference(dtoWithNullKey)).isNull();
        assertThat(mapper.drivetrainTypeReference(dtoWithNullKey)).isNull();
        assertThat(mapper.changeTypeReference(dtoWithNullKey)).isNull();
        assertThat(mapper.bodyTypeReference(dtoWithNullKey)).isNull();
        assertThat(mapper.colorReference(dtoWithNullKey)).isNull();
        assertThat(mapper.fuelTypeReference(dtoWithNullKey)).isNull();

        KeyValueDto validDto = new KeyValueDto("VAL", "Label");
        assertThat(mapper.colorReference(validDto).getId()).isEqualTo("VAL");
    }

    @Test
    @DisplayName("bodyTypeReference: Debe mapear correctamente un DTO válido a Entity")
    void bodyTypeReference_ShouldMapCorrectly() {
        // GIVEN
        KeyValueDto dto = new KeyValueDto();
        dto.setKey("SEDAN");
        dto.setValue("Turismo Sedan");

        // WHEN
        BodyTypesEntity result = mapper.bodyTypeReference(dto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("SEDAN");
    }

    @Test
    @DisplayName("changeTypeReference: Debe mapear el ID correctamente desde la clave del DTO")
    void changeTypeReference_ShouldMapCorrectly() {
        // GIVEN
        KeyValueDto dto = new KeyValueDto();
        dto.setKey("MANUAL");
        dto.setValue("Transmisión Manual");

        // WHEN
        ChangeTypesEntity result = mapper.changeTypeReference(dto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("MANUAL");
    }

    @Test
    @DisplayName("drivetrainTypeReference: Debe mapear el ID correctamente")
    void drivetrainTypeReference_ShouldMapCorrectly() {
        // GIVEN
        KeyValueDto dto = new KeyValueDto();
        dto.setKey("AWD");
        dto.setValue("All Wheel Drive");

        // WHEN
        DriveTrainTypeEntity result = mapper.drivetrainTypeReference(dto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("AWD");
    }

    @Test
    @DisplayName("environmentalBadgeReference: Debe mapear el ID correctamente")
    void environmentalBadgeReference_ShouldMapCorrectly() {
        // GIVEN
        KeyValueDto dto = new KeyValueDto();
        dto.setKey("ECO");
        dto.setValue("Etiqueta ECO");

        // WHEN
        EnvironmentalBadgeEntity result = mapper.environmentalBadgeReference(dto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("ECO");
    }

    @Test
    @DisplayName("equipmentReference: Debe mapear el ID correctamente desde el DTO")
    void equipmentReference_ShouldMapCorrectly() {
        // GIVEN
        KeyValueDto dto = new KeyValueDto();
        dto.setKey("ABS");
        dto.setValue("Sistema antibloqueo");

        // WHEN
        EquipmentsEntity result = mapper.equipmentReference(dto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("ABS");
    }

    @Test
    @DisplayName("equipmentCategoryReference: Debe mapear el ID correctamente")
    void equipmentCategoryReference_ShouldMapCorrectly() {
        // GIVEN
        KeyValueDto dto = new KeyValueDto();
        dto.setKey("SEGURIDAD");
        dto.setValue("Sistemas de seguridad");

        // WHEN
        EquipmentCategoryEntity result = mapper.equipmentCategoryReference(dto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("SEGURIDAD");
    }

    @Test
    @DisplayName("equipmentTypeReference: Debe mapear el ID correctamente")
    void equipmentTypeReference_ShouldMapCorrectly() {
        // GIVEN
        KeyValueDto dto = new KeyValueDto();
        dto.setKey("OPTIONAL");
        dto.setValue("Equipamiento Opcional");

        // WHEN
        EquipmentTypeEntity result = mapper.equipmentTypeReference(dto);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("OPTIONAL");
    }
}
