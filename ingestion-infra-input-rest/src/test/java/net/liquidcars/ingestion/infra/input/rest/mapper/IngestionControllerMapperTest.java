package net.liquidcars.ingestion.infra.input.rest.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferRequestFactory;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
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

    private final String participantId = UUID.randomUUID().toString();

    @Test
    void shouldMapOfferRequestToOfferDtoWithGeneratedId() {
        OfferRequest request = OfferRequestFactory.getOfferRequest();

        OfferDto result = mapper.toOfferDto(request, participantId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(request.getId());
        assertThat(result.getChannelReference()).isEqualTo(request.getChannelReference());
        assertThat(result.getVehicleInstance().getPlate()).isEqualTo(request.getVehicleInstance().getPlate());
        assertThat(result.getPickUpAddress().getAddress().getExtendedAddress())
                .isEqualTo(request.getPickUpAddress().getAddress().getExtendedAddress());
        assertThat(result.getMail()).isEqualTo(request.getMail());
        assertThat(result.getPrice().getAmount()).isEqualByComparingTo(request.getPrice().getAmount());
        assertThat(result.getParticipantId()).isEqualTo(UUID.fromString(participantId));
        assertThat(result.getLastUpdated()).isNotNull();
    }

    @Test
    void shouldMapListWithDifferentIds() {
        List<OfferRequest> requests = List.of(
                OfferRequestFactory.getOfferRequest(),
                OfferRequestFactory.getOfferRequest()
        );

        List<OfferDto> results = mapper.toOfferDtoList(requests, participantId);

        assertThat(results).hasSize(2);

        assertThat(results.get(0).getId())
                .isNotEqualTo(results.get(1).getId())
                .withFailMessage("Each DTO should have a unique ID");

        assertThat(results).extracting(OfferDto::getChannelReference)
                .containsExactly(requests.get(0).getChannelReference(), requests.get(1).getChannelReference());

        // Verify all items have the same participantId
        assertThat(results).extracting(OfferDto::getParticipantId)
                .containsOnly(UUID.fromString(participantId));

        // Verify all items have lastUpdated timestamp
        assertThat(results).allSatisfy(dto ->
                assertThat(dto.getLastUpdated()).isNotNull()
        );
    }


    @Test
    void shouldReturnNullWhenListIsNull() {
        List<OfferDto> result = mapper.toOfferDtoList(null, participantId);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnEmptyListWhenSourceListIsEmpty() {
        List<OfferRequest> emptyList = List.of();

        List<OfferDto> result = mapper.toOfferDtoList(emptyList, participantId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapParticipantIdCorrectly() {
        OfferRequest request = OfferRequestFactory.getOfferRequest();

        OfferDto result = mapper.toOfferDto(request, participantId);

        assertThat(result.getParticipantId())
                .isNotNull()
                .isEqualTo(UUID.fromString(participantId));
    }
}