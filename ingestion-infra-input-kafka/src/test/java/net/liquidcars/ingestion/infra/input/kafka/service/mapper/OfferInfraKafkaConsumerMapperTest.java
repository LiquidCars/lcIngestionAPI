package net.liquidcars.ingestion.infra.input.kafka.service.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferMsgFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {OfferInfraKafkaConsumerMapperImpl.class})
public class OfferInfraKafkaConsumerMapperTest {

    private final OfferInfraKafkaConsumerMapper mapper = new OfferInfraKafkaConsumerMapperImpl();

    @Test
    void shouldMapOfferMsgToOfferDto() {
        OfferMsg msg = OfferMsgFactory.getOfferMsg();

        OfferDto result = mapper.toOfferDto(msg);

        assertThat(result).isNotNull();

        if (msg.getVehicleType() != null) {
            assertThat(result.getVehicleType()).isNotNull();
            assertThat(result.getVehicleType().name()).isEqualTo(msg.getVehicleType().name());
        }

        if (msg.getStatus() != null) {
            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().name()).isEqualTo(msg.getStatus().name());
        }

        assertThat(result.getExternalId()).isEqualTo(msg.getExternalId());
        assertThat(result.getBrand()).isEqualTo(msg.getBrand());
        assertThat(result.getPrice()).isEqualByComparingTo(msg.getPrice());
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertThat(mapper.toOfferDto(null)).isNull();
    }

    @Test
    void testOfferMsgLogic() {
        OfferMsg msg = new OfferMsg();
        msg.setStatus(OfferMsg.OfferStatusMsg.ACTIVE);
        assertThat(msg.isAvailable()).isTrue();

        msg.setExternalId("EXT-1");
        msg.setBrand("Toyota");
        msg.setModel("Corolla");
        msg.setYear(2024);
        msg.setPrice(new BigDecimal("25000"));
        assertThat(msg.isValid()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(OfferMsg.VehicleTypeMsg.class)
    void shouldMapAllVehicleTypes(OfferMsg.VehicleTypeMsg type) {
        OfferMsg msg = new OfferMsg();
        msg.setVehicleType(type);

        OfferDto result = mapper.toOfferDto(msg);

        assertThat(result.getVehicleType().name()).isEqualTo(type.name());
    }

    @ParameterizedTest
    @EnumSource(OfferMsg.OfferStatusMsg.class)
    void shouldMapAllOfferStatuses(OfferMsg.OfferStatusMsg status) {
        OfferMsg msg = new OfferMsg();
        msg.setStatus(status);

        OfferDto result = mapper.toOfferDto(msg);

        assertThat(result.getStatus().name()).isEqualTo(status.name());
    }

    @Test
    void shouldHandleNullEnums() {
        OfferMsg msg = new OfferMsg();
        msg.setVehicleType(null);
        msg.setStatus(null);

        OfferDto result = mapper.toOfferDto(msg);

        assertThat(result.getVehicleType()).isNull();
        assertThat(result.getStatus()).isNull();
    }
}
