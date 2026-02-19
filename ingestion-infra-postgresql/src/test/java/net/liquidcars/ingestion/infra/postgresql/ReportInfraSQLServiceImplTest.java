package net.liquidcars.ingestion.infra.postgresql;

import net.liquidcars.ingestion.domain.model.SortDirection;
import net.liquidcars.ingestion.domain.model.batch.*;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionReportRepository;
import net.liquidcars.ingestion.infra.postgresql.service.ReportInfraSQLServiceImpl;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.ReportInfraSQLMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReportInfraSQLServiceImplTest {

    @Mock
    private ReportInfraSQLMapper mapper;

    @Mock
    private IngestionReportRepository reportRepository;

    @InjectMocks
    private ReportInfraSQLServiceImpl reportService;

    @Nested
    @DisplayName("Tests for upsertIngestionReport")
    class UpsertTests {
        @Test
        @DisplayName("Should save report successfully")
        void upsertSuccess() {
            IngestionReportDto dto = TestDataFactory.createIngestionReport();
            IngestionReportEntity entity = new IngestionReportEntity();

            when(mapper.toIngestionReportEntity(dto)).thenReturn(entity);

            reportService.upsertIngestionReport(dto);

            verify(reportRepository).save(entity);
        }

        @Test
        @DisplayName("Should throw LCIngestionException when repository fails")
        void upsertFailure() {
            IngestionReportDto dto = IngestionReportDto.builder().id(UUID.randomUUID()).build();
            when(mapper.toIngestionReportEntity(any())).thenReturn(new IngestionReportEntity());
            when(reportRepository.save(any())).thenThrow(new RuntimeException("DB Error"));

            assertThatThrownBy(() -> reportService.upsertIngestionReport(dto))
                    .isInstanceOf(LCIngestionException.class)
                    .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE);
        }
    }

    @Nested
    @DisplayName("Tests for findIngestionReportById")
    class FindByIdTests {
        @Test
        @DisplayName("Should return DTO when report exists")
        void findByIdSuccess() {
            UUID id = UUID.randomUUID();
            IngestionReportEntity entity = new IngestionReportEntity();
            IngestionReportDto dto = TestDataFactory.createIngestionReport();

            when(reportRepository.findById(id)).thenReturn(Optional.of(entity));
            when(mapper.toIngestionReportDto(entity)).thenReturn(dto);

            IngestionReportDto result = reportService.findIngestionReportById(id);

            assertThat(result).isEqualTo(dto);
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when report does not exist")
        void findByIdNotFound() {
            UUID id = UUID.randomUUID();
            when(reportRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.findIngestionReportById(id))
                    .isInstanceOf(LCIngestionException.class)
                    .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.NOT_FOUND);
        }
    }


    @Nested
    @DisplayName("Tests for existsByRequesterParticipantIdAndStatusNotIn")
    class ExistsTests {
        @Test
        @DisplayName("Should return boolean result from repository")
        void existsSuccess() {
            UUID participantId = UUID.randomUUID();
            List<IngestionBatchStatus> statuses = List.of(IngestionBatchStatus.COMPLETED);

            when(reportRepository.existsByRequesterParticipantIdAndStatusNotIn(participantId, statuses)).thenReturn(true);

            boolean result = reportService.existsByRequesterParticipantIdAndStatusNotIn(participantId, statuses);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should handle exception and format log message correctly")
        void existsFailure() {
            UUID participantId = UUID.randomUUID();
            when(reportRepository.existsByRequesterParticipantIdAndStatusNotIn(any(), any()))
                    .thenThrow(new RuntimeException("Error"));

            assertThatThrownBy(() -> reportService.existsByRequesterParticipantIdAndStatusNotIn(participantId, null))
                    .isInstanceOf(LCIngestionException.class)
                    .hasMessageContaining("SQL Failed to check");
        }
    }

    @Nested
    @DisplayName("Tests for getPendingReports")
    class PendingReportsTests {
        @Test
        @DisplayName("Should return list of pending reports")
        void getPendingReportsSuccess() {
            List<IngestionReportEntity> entities = List.of(new IngestionReportEntity());
            List<IngestionReportDto> dtos = List.of(TestDataFactory.createIngestionReport());

            when(reportRepository.findByProcessedFalse()).thenReturn(entities);
            when(mapper.toIngestionReportDtoList(entities)).thenReturn(dtos);

            List<IngestionReportDto> result = reportService.getPendingReports();

            assertThat(result).hasSize(1);
            assertThat(result).isEqualTo(dtos);
        }
    }

    @Nested
    @DisplayName("Tests for findIngestionReportById - Edge Cases")
    class FindByIdExtraTests {
        @Test
        @DisplayName("Should throw DATABASE tech cause when a generic exception occurs")
        void findByIdGenericException() {
            UUID id = UUID.randomUUID();
            when(reportRepository.findById(id)).thenThrow(new RuntimeException("Connection lost"));

            assertThatThrownBy(() -> reportService.findIngestionReportById(id))
                    .isInstanceOf(LCIngestionException.class)
                    .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE);
        }
    }

    @Nested
    @DisplayName("Tests for findIngestionReports")
    class FindReportsTests {
        @Test
        @DisplayName("Should return paginated reports successfully")
        void findIngestionReportsSuccess() {
            IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                    .page(0)
                    .size(10)
                    .sortDirection(SortDirection.ASC)
                    .sortBy(IngestionReportSortField.id)
                    .build();

            Page<IngestionReportEntity> entityPage = new PageImpl<>(List.of(new IngestionReportEntity()));
            IngestionReportPageDto expectedDto = IngestionReportPageDto.builder().build();

            when(reportRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
            when(mapper.toIngestionReportPageDto(entityPage)).thenReturn(expectedDto);

            IngestionReportPageDto result = reportService.findIngestionReports(filter);

            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("Should throw DATABASE exception when query fails")
        void findIngestionReportsFailure() {
            // Debemos setear page, size y sortDirection para evitar NPE en el servicio
            IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                    .page(0)
                    .size(10)
                    .sortDirection(SortDirection.DESC)
                    .sortBy(IngestionReportSortField.id) // Usa el enum correcto de tu dominio
                    .build();

            // Ahora sí llegará a ejecutar el repositorio
            when(reportRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenThrow(new RuntimeException("Query error"));

            assertThatThrownBy(() -> reportService.findIngestionReports(filter))
                    .isInstanceOf(LCIngestionException.class)
                    .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                    .hasMessageContaining("Error querying ingestion reports database");
        }
    }

    @Nested
    @DisplayName("Tests for findIngestionReportByBatchJobId")
    class FindByBatchJobIdTests {
        @Test
        @DisplayName("Should return DTO when batchJobId exists")
        void findByBatchJobIdSuccess() {
            UUID batchId = UUID.randomUUID();
            IngestionReportEntity entity = new IngestionReportEntity();
            IngestionReportDto dto = IngestionReportDto.builder().id(UUID.randomUUID()).build();

            when(reportRepository.findByBatchJobId(batchId)).thenReturn(Optional.of(entity));
            when(mapper.toIngestionReportDto(entity)).thenReturn(dto);

            IngestionReportDto result = reportService.findIngestionReportByBatchJobId(batchId);

            assertThat(result).isEqualTo(dto);
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when batchJobId does not exist")
        void findByBatchJobIdNotFound() {
            UUID batchId = UUID.randomUUID();
            when(reportRepository.findByBatchJobId(batchId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.findIngestionReportByBatchJobId(batchId))
                    .isInstanceOf(LCIngestionException.class)
                    .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw DATABASE exception on generic error")
        void findByBatchJobIdGenericError() {
            UUID batchId = UUID.randomUUID();
            when(reportRepository.findByBatchJobId(batchId)).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> reportService.findIngestionReportByBatchJobId(batchId))
                    .isInstanceOf(LCIngestionException.class)
                    .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE);
        }
    }

    @Nested
    @DisplayName("Tests for existsByRequesterParticipantIdAndStatusNotIn - Edge Cases")
    class ExistsExtraTests {
        @Test
        @DisplayName("Should format log message with status list when statuses are provided and error occurs")
        void existsFailureWithStatuses() {
            UUID participantId = UUID.randomUUID();
            List<IngestionBatchStatus> statuses = List.of(IngestionBatchStatus.FAILED, IngestionBatchStatus.COMPLETED);

            when(reportRepository.existsByRequesterParticipantIdAndStatusNotIn(any(), any()))
                    .thenThrow(new RuntimeException("Error"));

            assertThatThrownBy(() -> reportService.existsByRequesterParticipantIdAndStatusNotIn(participantId, statuses))
                    .isInstanceOf(LCIngestionException.class)
                    .hasMessageContaining("FAILED, COMPLETED");
        }

    }
}
