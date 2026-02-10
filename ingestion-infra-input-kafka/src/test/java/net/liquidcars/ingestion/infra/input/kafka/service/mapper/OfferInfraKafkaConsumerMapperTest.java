package net.liquidcars.ingestion.infra.input.kafka.service.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferMsgFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest(classes = {OfferInfraKafkaConsumerMapperImpl.class})
public class OfferInfraKafkaConsumerMapperTest {

    private final OfferInfraKafkaConsumerMapper mapper = new OfferInfraKafkaConsumerMapperImpl();

    @Test
    void shouldMapOfferMsgToOfferDto() {
        OfferMsg msg = OfferMsgFactory.getOfferMsg();

        OfferDto result = mapper.toOfferDto(msg);

        assertThat(result).isNotNull();
        assertThat(result.getOwnerReference()).isEqualTo(msg.getOwnerReference());

        if (msg.getVehicleInstance() != null && msg.getVehicleInstance().getVehicleModel() != null) {
            assertThat(result.getVehicleInstance().getPlate()).isEqualTo(msg.getVehicleInstance().getPlate());
            assertThat(result.getVehicleInstance().getVehicleModel().getModel()).isEqualTo(msg.getVehicleInstance().getVehicleModel().getModel());
        }

        if (msg.getPrice() != null) {
            assertThat(result.getPrice().getAmount()).isEqualByComparingTo(msg.getPrice().getAmount());
        }
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertThat(mapper.toOfferDto(null)).isNull();
    }

    @Test
    void shouldHandleMissingNestedObjects() {
        // Test manual para asegurar que el mapper no rompe si falta una rama del árbol
        OfferMsg msg = OfferMsgFactory.getOfferMsg();
        msg.setVehicleInstance(null);

        OfferDto result = mapper.toOfferDto(msg);

        assertThat(result).isNotNull();
        assertThat(result.getChannelReference()).isNull();
    }
}