package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.application.service.batch.mapper.IngestionBatchMapper;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobCompletionNotificationListenerTest {

    @Mock
    private IOfferInfraKafkaProducerService kafkaProducer;
    @Mock
    private JobFailedIdsCollector failedIdsCollector;
    @Mock
    private IngestionBatchMapper mapper;

    @InjectMocks
    private JobCompletionNotificationListener listener;

    private JobExecution jobExecution;
    private UUID ingestionId;

    @BeforeEach
    void setUp() {
        ingestionId = UUID.randomUUID();
        JobParameters params = new JobParametersBuilder()
                .addString("ingestionId", ingestionId.toString())
                .toJobParameters();

        jobExecution = new JobExecution(1L, params);
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setStartTime(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void shouldSendReportSuccessfully_WhenJobFinishes() {
        StepExecution step1 = new StepExecution("step1", jobExecution);
        step1.setReadCount(10);
        step1.setWriteCount(8);
        step1.setWriteSkipCount(2);

        StepExecution step2 = new StepExecution("step2", jobExecution);
        step2.setReadCount(5);
        step2.setWriteCount(5);
        step2.setWriteSkipCount(0);

        jobExecution.addStepExecutions(List.of(step1, step2));

        when(mapper.toIngestionBatchStatus(BatchStatus.COMPLETED)).thenReturn(IngestionBatchStatus.COMPLETED);

        JobDeleteExternalIdsCollector deleteCollector = new JobDeleteExternalIdsCollector();
        deleteCollector.addId("DEL-1");
        jobExecution.getExecutionContext().put("deleteExternalIdsCollector", deleteCollector);

        listener.afterJob(jobExecution);

        ArgumentCaptor<IngestionBatchReportDto> reportCaptor = ArgumentCaptor.forClass(IngestionBatchReportDto.class);
        verify(kafkaProducer).sendBatchIngestionJobReport(reportCaptor.capture());

        IngestionBatchReportDto capturedReport = reportCaptor.getValue();
        assertThat(capturedReport.getJobId()).isEqualTo(ingestionId);
        assertThat(capturedReport.getReadCount()).isEqualTo(15);
        assertThat(capturedReport.getWriteCount()).isEqualTo(13);
        assertThat(capturedReport.getSkipCount()).isEqualTo(2);
        assertThat(capturedReport.getIdsForDelete()).containsExactly("DEL-1");

        verify(failedIdsCollector).clear();
    }

    @Test
    void shouldNotSendReport_WhenIngestionIdIsNull() {
        JobExecution jobNoParams = new JobExecution(2L, new JobParameters());

        listener.afterJob(jobNoParams);

        verifyNoInteractions(kafkaProducer);
        verifyNoInteractions(mapper);
    }

    @Test
    void shouldNotProcess_WhenStatusIsStarting() {
        jobExecution.setStatus(BatchStatus.STARTING);

        listener.afterJob(jobExecution);

        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void shouldHandleNullDeleteCollectorGracefully() {
        StepExecution step = new StepExecution("step", jobExecution);
        jobExecution.addStepExecutions(List.of(step));

        when(mapper.toIngestionBatchStatus(any())).thenReturn(IngestionBatchStatus.FAILED);

        listener.afterJob(jobExecution);

        verify(kafkaProducer).sendBatchIngestionJobReport(argThat(report ->
                report.getIdsForDelete() == null
        ));
    }
}
