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

    private final UUID participantId = UUID.randomUUID();
    private final UUID inventoryId = UUID.randomUUID();

    @Test
    void shouldMapOfferRequestToOfferDtoWithGeneratedId() {
        OfferRequest request = OfferRequestFactory.getOfferRequest();

        OfferDto result = mapper.toOfferDto(request, participantId, inventoryId);

        assertThat(result).isNotNull();
        assertThat(result.getExternalIdInfo().getChannelReference()).isEqualTo(request.getExternalIdInfo().getChannelReference());
        assertThat(result.getVehicleInstance().getPlate()).isEqualTo(request.getVehicleInstance().getPlate());

        assertThat(result.getMail()).isEqualTo(request.getMail());
        assertThat(result.getPrice().getAmount()).isEqualByComparingTo(request.getPrice().getAmount());
        assertThat(result.getParticipantId()).isEqualTo(participantId);
        assertThat(result.getInventoryId()).isEqualTo(inventoryId);
        assertThat(result.getLastUpdated()).isNotNull();
    }

    @Test
    void shouldReturnNullWhenListIsNull() {
        List<OfferDto> result = mapper.toOfferDtoList(null, participantId, inventoryId);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnEmptyListWhenSourceListIsEmpty() {
        List<OfferRequest> emptyList = List.of();

        List<OfferDto> result = mapper.toOfferDtoList(emptyList, participantId, inventoryId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapParticipantIdCorrectly() {
        OfferRequest request = OfferRequestFactory.getOfferRequest();

        OfferDto result = mapper.toOfferDto(request, participantId, inventoryId);

        assertThat(result.getParticipantId())
                .isNotNull()
                .isEqualTo(participantId);
    }
}