package net.liquidcars.ingestion.application.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionParserException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing(modular = true)
public class IngestionBatchConfig {


    @Value("${ingestion.batch.chunk-size:10}")
    private int chunkSize;

    @Value("${ingestion.batch.skip-limit:100}")
    private int skipLimit;

    @Bean
    public Job offerIngestionJob(JobRepository jobRepository, Step ingestionStep, JobCompletionNotificationListener jobCompletionListener) {
        return new JobBuilder("offerIngestionJob", jobRepository)
                .listener(jobCompletionListener)
                .start(ingestionStep)
                .build();
    }


    @Bean
    public Step ingestionStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            OfferStreamItemReader offerReader,
            OfferItemWriter offerItemWriter,
            IngestionSkipListener ingestionSkipListener,
            JobFailedIdsCollector failedIdsCollector,
            JobDeleteExternalIdsCollector deleteExternalIdsCollector
    ){
        return new StepBuilder("ingestionStep", jobRepository)
                .<OfferDto, OfferDto>chunk(chunkSize, transactionManager)
                .reader(offerReader)
                .writer(offerItemWriter)
                .faultTolerant()
                /* * 1. RETRY STRATEGY
                 * We retry for infrastructure-related issues (Kafka, Network, Database).
                 */
                .retry(LCIngestionException.class)
                .retryLimit(3)
                /* * 2. BACKOFF STRATEGY
                 * Adds a delay between retries to allow the infrastructure (Kafka) to recover.
                 * Initial wait: 2 seconds, then doubles each time (2s, 4s, 8s).
                 */
                .backOffPolicy(new ExponentialBackOffPolicy() {{
                    setInitialInterval(2000);
                    setMultiplier(2.0);
                }})
                /* * 3. SKIP STRATEGY
                 * Determines whether to skip a record or fail the entire Job
                 * after retries are exhausted or if the error is non-retryable.
                 */
                .skipPolicy((t, skipCount) -> {

                    if (t instanceof LCIngestionParserException ex && ex.getFailedIdentifier() != null) {
                        failedIdsCollector.addId(ex.getFailedIdentifier());
                    }

                    if (skipCount > skipLimit) {
                        log.error("Skip limit exceeded! Failing job.");
                        return false;
                    }

                    if (t instanceof LCIngestionException ex) {
                        boolean isDataError = ex.getTechCause() == LCTechCauseEnum.CONVERSION_ERROR;
                        if (isDataError) {
                            log.debug("SkipPolicy: Skipping record due to conversion error. Skip count: {}", skipCount);
                            return true;
                        }
                        // If it's a DATABASE/INTERNAL_ERROR (Kafka down), we return false to STOP the Job
                        log.error("SkipPolicy: Critical infrastructure error detected. Failing Job.");
                        return false;
                    }
                    // For any other unexpected exception, do not skip
                    return false;
                })
                /* * 4. LISTENERS
                 * Used for auditing and logging skipped records.
                 */
                .listener(ingestionSkipListener)
                .build();
    }
}