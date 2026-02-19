package net.liquidcars.ingestion.infra.postgresql.mapper;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionBatchReportEntity;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.BatchReportInfraSQLMapper;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.BatchReportInfraSQLMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportInfraSQLMapperTest {

    private BatchReportInfraSQLMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BatchReportInfraSQLMapperImpl();
    }

    @Test
    @DisplayName("Should map DTO to Entity successfully")
    void toIngestionReportEntitySuccess() {
        IngestionBatchReportDto dto = TestDataFactory.createIngestionBatchReportDto();

        IngestionBatchReportEntity entity = mapper.toIngestionReportEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getJobId()).isEqualTo(dto.getJobId());
    }

    @Test
    @DisplayName("Should return null when mapping null DTO to Entity")
    void toIngestionReportEntityNull() {
        assertThat(mapper.toIngestionReportEntity(null)).isNull();
    }

    @Test
    @DisplayName("Should map Entity List to DTO List successfully")
    void toIngestionReportDtoListSuccess() {
        List<IngestionBatchReportEntity> entities = TestDataFactory.createIngestionBatchReportEntityList(3);

        List<IngestionBatchReportDto> dtos = mapper.toIngestionReportDtoList(entities);

        assertThat(dtos).isNotNull().hasSize(3);
        assertThat(dtos.get(0).getJobId()).isEqualTo(entities.get(0).getJobId());
    }

    @Test
    @DisplayName("Should return null when mapping null Entity List")
    void toIngestionReportDtoListNull() {
        assertThat(mapper.toIngestionReportDtoList(null)).isNull();
    }
}
