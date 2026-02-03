package net.liquidcars.ingestion.infra.input.rest.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferRequestFactory;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
import net.liquidcars.ingestion.infra.input.rest.model.OfferStatus;
import net.liquidcars.ingestion.infra.input.rest.model.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {IngestionControllerMapperImpl.class})
public class IngestionControllerMapperTest {

    @Autowired
    private IngestionControllerMapper mapper;

    @Test
    void shouldMapOfferRequestToOfferDtoWithGeneratedId() {
        OfferRequest request = OfferRequestFactory.getOfferRequest();

        OfferDto result = mapper.toOfferDto(request);

        assertThat(result).isNotNull();

        assertThat(result.getBrand()).isEqualTo(request.getBrand());
        assertThat(result.getModel()).isEqualTo(request.getModel());
        assertThat(result.getPrice()).isEqualByComparingTo(request.getPrice());

        assertThat(result.getId())
                .isNotNull()
                .satisfies(id -> assertThat(UUID.fromString(id)).isNotNull());
    }

    @Test
    void shouldMapListWithDifferentIds() {
        List<OfferRequest> requests = List.of(
                OfferRequestFactory.getOfferRequest(),
                OfferRequestFactory.getOfferRequest()
        );

        List<OfferDto> results = mapper.toOfferDtoList(requests);

        assertThat(results).hasSize(2);

        assertThat(results.get(0).getId())
                .isNotEqualTo(results.get(1).getId())
                .withFailMessage("Cada DTO debería tener un UUID único generado por el mapper");

        assertThat(results).extracting(OfferDto::getBrand)
                .containsExactly(requests.get(0).getBrand(), requests.get(1).getBrand());
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        OfferDto result = mapper.toOfferDto(null);

        assertThat(result).isNull();
    }

    @Test
    void shouldMapVehicleType() {
        // Given - Usando un valor real de tu Enum de origen
        VehicleType type = VehicleType.CAR;

        // When
        OfferDto.VehicleTypeDto result = mapper.mapVehicleType(type);

        // Then
        assertThat(result).isEqualTo(OfferDto.VehicleTypeDto.CAR);
    }

    @Test
    void shouldMapOfferStatus() {
        // Given
        OfferStatus status = OfferStatus.ACTIVE;

        // When
        OfferDto.OfferStatusDto result = mapper.mapOfferStatus(status);

        // Then
        assertThat(result).isEqualTo(OfferDto.OfferStatusDto.ACTIVE);
    }

    @Test
    void shouldMapFullRequestWithEnums() {
        // Given
        OfferRequest request = OfferRequestFactory.getOfferRequest();
        // Forzamos un valor conocido si queremos estar 100% seguros
        request.setVehicleType(VehicleType.SUV);

        // When
        OfferDto result = mapper.toOfferDto(request);

        // Then
        assertThat(result.getVehicleType()).isEqualTo(OfferDto.VehicleTypeDto.SUV);
        assertThat(result.getStatus()).isNotNull();
    }

    @Test
    void shouldMapOfferStatusToNull() {
        // Jacoco necesita ver que el 'if (status == null)' se ejecuta
        assertThat(mapper.mapOfferStatus(null)).isNull();
    }

    @Test
    void shouldMapVehicleTypeToNull() {
        // Jacoco necesita ver que el 'if (vehicleType == null)' se ejecuta
        assertThat(mapper.mapVehicleType(null)).isNull();
    }

    @Test
    void shouldReturnNullWhenListIsNull() {
        // Esta es la razón del 50% en toOfferDtoList
        assertThat(mapper.toOfferDtoList(null)).isNull();
    }

    @Test
    void shouldMapAllOfferStatusValues() {
        // Para cubrir el 100% de las ramas del switch de Enums
        for (OfferStatus status : OfferStatus.values()) {
            OfferDto.OfferStatusDto result = mapper.mapOfferStatus(status);
            assertThat(result.name()).isEqualTo(status.name());
        }
    }

    @Test
    void shouldMapAllVehicleTypeValues() {
        // Para cubrir todas las opciones del enum VehicleType
        for (VehicleType type : VehicleType.values()) {
            OfferDto.VehicleTypeDto result = mapper.mapVehicleType(type);
            assertThat(result.name()).isEqualTo(type.name());
        }
    }
}
