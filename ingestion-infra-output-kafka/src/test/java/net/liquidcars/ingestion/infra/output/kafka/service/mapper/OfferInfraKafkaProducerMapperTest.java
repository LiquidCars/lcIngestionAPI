package net.liquidcars.ingestion.infra.output.kafka.service.mapper;

import net.liquidcars.ingestion.domain.model.IngestionReportResponseActionDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.OfferSummaryDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;


public class OfferInfraKafkaProducerMapperTest {

    private final OfferInfraKafkaProducerMapper mapper = Mappers.getMapper(OfferInfraKafkaProducerMapper.class);

    @Test
    @DisplayName("Should map OfferDto to OfferMsg")
    void toOfferMsg_ShouldMapAllFields() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferMsg result = mapper.toOfferMsg(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(dto.getId());
    }

    @Test
    @DisplayName("Should map BatchIngestionReportDto to Msg including formatted dates")
    void toBatchIngestionReportMsg_ShouldMapDates() {
        IngestionBatchReportDto dto = TestDataFactory.createIngestionBatchReportDto();

        BatchIngestionReportMsg result = mapper.toBatchIngestionReportMsg(dto);

        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(dto.getJobId().toString());
        assertThat(result.getStartTime()).isNotNull();
    }

    @Test
    @DisplayName("Should map IngestionReportDto to IngestionReportMsg")
    void toIngestionReportMsg_ShouldMapAllFields() {
        IngestionReportDto dto = TestDataFactory.createIngestionReport();
        IngestionReportMsg result = mapper.toIngestionReportMsg(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId().toString()).isEqualTo(dto.getId().toString());
    }

    @Test
    @DisplayName("Should map OfferSummaryDto to OfferSummaryMsg")
    void toOfferSummaryMsg_ShouldMapFields() {
        OfferSummaryDto dto = TestDataFactory.createOfferSummaryDto();
        OfferSummaryMsg result = mapper.toOfferSummaryMsg(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(dto.getId());
        assertThat(result.getHash()).isEqualTo(dto.getHash());
    }

    @Test
    @DisplayName("Should map ResponseActionDto to ResponseActionMsg")
    void toIngestionReportResponseActionMsg_ShouldMapFields() {
        IngestionReportResponseActionDto dto = TestDataFactory.createIngestionReportResponseActionDto();
        IngestionReportResponseActionMsg result = mapper.toIngestionReportResponseActionMsg(dto);

        assertThat(result).isNotNull();
        assertThat(result.getIngestionReportId()).isEqualTo(dto.getIngestionReportId());
    }

    @Test
    @DisplayName("Default map method should format OffsetDateTime to String")
    void map_OffsetDateTimeToString_ShouldFormatCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();
        String result = mapper.map(now);

        assertThat(result).isEqualTo(now.format(OfferInfraKafkaProducerMapper.OFFSET_FORMATTER));
    }

    @Test
    @DisplayName("Should handle all null inputs for 100% coverage")
    void shouldHandleNullInputs() {
        assertThat(mapper.toOfferMsg(null)).isNull();
        assertThat(mapper.toBatchIngestionReportMsg(null)).isNull();
        assertThat(mapper.toIngestionReportMsg(null)).isNull();
        assertThat(mapper.toOfferSummaryMsg(null)).isNull();
        assertThat(mapper.toIngestionReportResponseActionMsg(null)).isNull();
        assertThat(mapper.map(null)).isNull();
    }

}
