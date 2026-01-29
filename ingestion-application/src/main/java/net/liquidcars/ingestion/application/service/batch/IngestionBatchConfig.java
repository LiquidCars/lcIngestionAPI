package net.liquidcars.ingestion.application.service.batch;

import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.domain.model.OfferDto;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class IngestionBatchConfig {

    private final OfferItemWriter offerItemWriter;
    @Value("${ingestion.batch.chunk-size:10}")
    private int chunkSize;

    @Value("${ingestion.batch.skip-limit:100}")
    private int skipLimit;

    @Bean
    public Job offerIngestionJob(JobRepository jobRepository, Step ingestionStep) {
        return new JobBuilder("offerIngestionJob", jobRepository)
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
                .skipLimit(skipLimit)
                .skip(Exception.class)
                .build();
    }
}