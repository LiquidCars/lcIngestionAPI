package net.liquidcars.ingestion.infra.postgresql;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionBatchReportEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionBatchReportRepository;
import net.liquidcars.ingestion.infra.postgresql.service.BatchReportInfraSQLServiceImpl;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.BatchReportInfraSQLMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BatchReportInfraSQLServiceImplTest {

    @Mock
    private BatchReportInfraSQLMapper mapper;

    @Mock
    private IngestionBatchReportRepository reportRepository;

    @InjectMocks
    private BatchReportInfraSQLServiceImpl batchReportService;

    @Nested
    @DisplayName("Tests for getBatchPendingReports")
    class GetBatchPendingReportsTests {

        @Test
        @DisplayName("Should return a list of pending reports successfully")
        void getBatchPendingReportsSuccess() {
            // Arrange
            IngestionBatchReportEntity entity = new IngestionBatchReportEntity();
            IngestionBatchReportDto dto = IngestionBatchReportDto.builder().build();

            when(reportRepository.findByProcessedFalse()).thenReturn(List.of(entity));
            when(mapper.toIngestionReportDtoList(anyList())).thenReturn(List.of(dto));

            // Act
            List<IngestionBatchReportDto> result = batchReportService.getBatchPendingReports();

            // Assert
            assertThat(result).hasSize(1).containsExactly(dto);
            verify(reportRepository).findByProcessedFalse();
        }

        @Test
        @DisplayName("Should return empty list when no pending reports exist")
        void getBatchPendingReportsEmpty() {
            when(reportRepository.findByProcessedFalse()).thenReturn(Collections.emptyList());
            when(mapper.toIngestionReportDtoList(anyList())).thenReturn(Collections.emptyList());

            List<IngestionBatchReportDto> result = batchReportService.getBatchPendingReports();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests for upsertIngestionBatchReport")
    class UpsertTests {

        @Test
        @DisplayName("Should save batch report successfully")
        void upsertSuccess() {
            // Arrange
            IngestionBatchReportDto dto = IngestionBatchReportDto.builder()
                    .jobId(UUID.randomUUID())
                    .build();
            IngestionBatchReportEntity entity = new IngestionBatchReportEntity();

            when(mapper.toIngestionReportEntity(dto)).thenReturn(entity);

            // Act
            batchReportService.upsertIngestionBatchReport(dto);

            // Assert
            verify(reportRepository, times(1)).save(entity);
        }

        @Test
        @DisplayName("Should throw LCIngestionException when repository fails")
        void upsertFailure() {
            // Arrange
            UUID jobId = UUID.randomUUID();
            IngestionBatchReportDto dto = IngestionBatchReportDto.builder()
                    .jobId(jobId)
                    .build();

            when(mapper.toIngestionReportEntity(any())).thenReturn(new IngestionBatchReportEntity());
            when(reportRepository.save(any())).thenThrow(new RuntimeException("Data Integrity Violation"));

            // Act & Assert
            assertThatThrownBy(() -> batchReportService.upsertIngestionBatchReport(dto))
                    .isInstanceOf(LCIngestionException.class)
                    .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                    .hasMessageContaining("SQL persistence error for batch report with id: " + jobId);

            verify(reportRepository).save(any());
        }
    }
}
