package net.liquidcars.ingestion.infra.postgresql.mapper;

import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportPageDto;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.ReportInfraSQLMapper;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.ReportInfraSQLMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportInfraSQLMapperTest {

    private ReportInfraSQLMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReportInfraSQLMapperImpl();
    }

    @Test
    @DisplayName("Should map DTO to Entity and vice versa")
    void testSimpleMapping() {
        IngestionReportDto dto = TestDataFactory.createIngestionReport();

        IngestionReportEntity entity = mapper.toIngestionReportEntity(dto);
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(dto.getId());

        IngestionReportDto mappedDto = mapper.toIngestionReportDto(entity);
        assertThat(mappedDto.getId()).isEqualTo(entity.getId());
    }

    @Test
    @DisplayName("Should map list of entities to list of DTOs")
    void testListMapping() {
        List<IngestionReportEntity> entities = List.of(TestDataFactory.createIngestionReportEntity());

        List<IngestionReportDto> dtos = mapper.toIngestionReportDtoList(entities);

        assertThat(dtos).hasSameSizeAs(entities);
        assertThat(mapper.toIngestionReportDtoList(null)).isNull();
    }

    @Test
    @DisplayName("Should map Page to PageDto correctly (100% Coverage)")
    void testPageMapping() {
        Page<IngestionReportEntity> entityPage = TestDataFactory.createIngestionReportPage(5);

        IngestionReportPageDto result = mapper.toIngestionReportPageDto(entityPage);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getNumber()).isZero();

        assertThat(mapper.toIngestionReportPageDto(null)).isNull();
    }
}
