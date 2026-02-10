package net.liquidcars.ingestion.infra.output.kafka.model;

import net.liquidcars.ingestion.factory.OfferMsgFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferMsgTest {

    @Test
    @DisplayName("Should create a valid OfferMsg using the factory")
    void factoryShouldProduceValidObject() {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();

        assertThat(offer).isNotNull();
        assertThat(offer.getId()).isNotNull();
    }

}
