package net.liquidcars.ingestion.infra.input.kafka.service.mapper;

import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.factory.OfferMsgFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.BatchIngestionReportMsg;
import net.liquidcars.ingestion.infra.output.kafka.model.ExternalIdInfoMsg;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = {OfferInfraKafkaConsumerMapperImpl.class})
public class OfferInfraKafkaConsumerMapperTest {

    private final OfferInfraKafkaConsumerMapper mapper = new OfferInfraKafkaConsumerMapperImpl();

    @Test
    void shouldMapOfferMsgToOfferDto() {
        OfferMsg msg = OfferMsgFactory.getOfferMsg();

        OfferDto result = mapper.toOfferDto(msg);

        assertThat(result).isNotNull();
        assertThat(result.getExternalIdInfo().getOwnerReference()).isEqualTo(msg.getExternalIdInfo().getOwnerReference());

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
        msg.setExternalIdInfo(null);

        OfferDto result = mapper.toOfferDto(msg);

        assertThat(result).isNotNull();
        assertThat(result.getExternalIdInfo()).isNull();
    }

    @Test
    @DisplayName("Should map BatchIngestionReportMsg to IngestionBatchReportDto correctly including dates and lists")
    void toIngestionReportDto_ShouldMapAllFields() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String startTimeStr = "2026-02-18T10:00:00.000+01:00";
        String endTimeStr = "2026-02-18T11:00:00.000+01:00";

        // Ajustado a los campos reales de tu ExternalIdInfoMsg
        ExternalIdInfoMsg externalIdMsg = ExternalIdInfoMsg.builder()
                .ownerReference("OWN-123")
                .dealerReference("DLR-456")
                .channelReference("CH-789")
                .build();

        BatchIngestionReportMsg message = new BatchIngestionReportMsg();
        message.setJobId(jobId.toString());
        message.setReadCount(100L);
        message.setWriteCount(80L);
        message.setStartTime(startTimeStr);
        message.setEndTime(endTimeStr);
        message.setFailedExternalIds(List.of(externalIdMsg));

        // Act
        IngestionBatchReportDto result = mapper.toIngestionReportDto(message);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getJobId()).isEqualTo(jobId);
        assertThat(result.getReadCount()).isEqualTo(100L);

        // Verificación de mapeo de fechas con tu OFFSET_FORMATTER
        assertThat(result.getStartTime()).isEqualTo(OffsetDateTime.parse(startTimeStr));
        assertThat(result.getEndTime()).isEqualTo(OffsetDateTime.parse(endTimeStr));

        // Verificación de mapeo de listas
        assertThat(result.getFailedExternalIds()).hasSize(1);
        ExternalIdInfoDto listResult = result.getFailedExternalIds().get(0);
        assertThat(listResult.getOwnerReference()).isEqualTo("OWN-123");
        assertThat(listResult.getDealerReference()).isEqualTo("DLR-456");
    }

    @Test
    @DisplayName("Should return null and handle empty lists when source message is null")
    void toIngestionReportDto_ShouldHandleNullSource() {
        // Act
        IngestionBatchReportDto result = mapper.toIngestionReportDto(null);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle null date strings gracefully")
    void map_ShouldReturnNull_WhenDateStringIsNull() {
        // Act
        OffsetDateTime result = mapper.map((String) null);

        // Assert
        assertNull(result);
    }
}