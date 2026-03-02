package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.instancio.Instancio;

public class OfferDtoFactory {

    public static OfferDto getOfferDto() {
       return Instancio.create(OfferDto.class);
    }

}
