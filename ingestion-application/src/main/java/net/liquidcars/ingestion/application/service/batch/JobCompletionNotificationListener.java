package net.liquidcars.ingestion.application.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {

    private final IOfferInfraKafkaProducerService kafkaProducer;
    private final JobFailedIdsCollector failedIdsCollector;

    @Override
    public void afterJob(JobExecution jobExecution) {
        // We only care about final states
        String ingestionId = jobExecution.getJobParameters()
                .getString("ingestionId");
        if (ingestionId == null) {
            log.warn("Skipping ingestion report: ingestionId is null");
            return;
        }
        if (jobExecution.getStatus() != BatchStatus.STARTING) {
            // 1. Calculate metrics
            long readCount = 0, writeCount = 0, skipCount = 0;
            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                readCount += stepExecution.getReadCount();
                writeCount += stepExecution.getWriteCount();
                skipCount += stepExecution.getSkipCount();
            }

            // 2. Build report with Start and End times
            IngestionReportDto report = IngestionReportDto.builder()
                    .jobId(jobExecution.getJobInstance().getJobName() + "-" + ingestionId)
                    .status(jobExecution.getStatus().toString())
                    .readCount(readCount)
                    .writeCount(writeCount)
                    .skipCount(skipCount)
                    .failedExternalIds(failedIdsCollector.getFailedIds())
                    .startTime(Optional.ofNullable(jobExecution.getStartTime())
                            .map(startTime -> startTime.atZone(ZoneId.systemDefault()).toOffsetDateTime())
                            .orElse(OffsetDateTime.now()))
                    .endTime(OffsetDateTime.now())
                    .build();

            long durationSeconds = java.time.Duration.between(report.getStartTime(), report.getEndTime()).toSeconds();

            log.info(">> Job finished: {} | Status: {} | Duration: {}s | Read: {} | Written: {} | Skipped: {}",
                    report.getJobId(),
                    report.getStatus(),
                    durationSeconds,
                    readCount,
                    writeCount,
                    skipCount);

            log.info(">> Sending Job Report for Job: {}", report.getJobId());
            kafkaProducer.sendJobReport(report);

            failedIdsCollector.clear();
        }
    }
}