package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.application.service.batch.mapper.IngestionBatchMapper;
import net.liquidcars.ingestion.application.service.batch.mapper.IngestionBatchMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.batch.core.BatchStatus;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class IngestionBatchMapperTest {

    private final IngestionBatchMapper mapper = new IngestionBatchMapperImpl();

    @ParameterizedTest
    @EnumSource(BatchStatus.class)
    @DisplayName("Debe mapear todos los estados de Spring Batch a IngestionBatchStatus")
    void shouldMapAllBatchStatuses(BatchStatus status) {
        // Act
        IngestionBatchStatus result = mapper.toIngestionBatchStatus(status);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(status.name());
    }

    @Test
    @DisplayName("Debe retornar null cuando el status es null")
    void shouldReturnNullWhenStatusIsNull() {
        // Act
        IngestionBatchStatus result = mapper.toIngestionBatchStatus(null);

        // Assert
        assertThat(result).isNull();
    }

}
