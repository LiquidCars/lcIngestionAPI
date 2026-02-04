package net.liquidcars.ingestion.application.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
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
public class IngestionBatchConfig {

    private final OfferItemWriter offerItemWriter;
    private IngestionSkipListener ingestionSkipListener;

    @Value("${ingestion.batch.chunk-size:10}")
    private int chunkSize;

    @Value("${ingestion.batch.skip-limit:100}")
    private int skipLimit;

    @Bean
    public Job offerIngestionJob(JobRepository jobRepository, Step ingestionStep) {
        return new JobBuilder("offerIngestionJob", jobRepository)
                .incrementer(new RunIdIncrementer()) //Allows rerun the job with same name
                .start(ingestionStep)
                .build();
    }


    @Bean
    public Step ingestionStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("ingestionStep", jobRepository)
                .<OfferDto, OfferDto>chunk(chunkSize, transactionManager)
                .reader(() -> null)
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
                .skipLimit(skipLimit)
                .skipPolicy((t, skipCount) -> {
                    // We only skip if it's a known data conversion error (bad JSON/XML)
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