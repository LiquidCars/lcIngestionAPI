package net.liquidcars.ingestion.application.service.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class IngestionBatchConfigTest {

    @Mock
    private OfferItemWriter offerItemWriter;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private IngestionSkipListener ingestionSkipListener;

    @Mock
    private JobCompletionNotificationListener jobCompletionListener;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private OfferStreamItemReader offerReader;

    @InjectMocks
    private IngestionBatchConfig ingestionBatchConfig;

    @Test
    void shouldCreateOfferIngestionJob() {
        Step mockStep = mock(Step.class);

        Job job = ingestionBatchConfig.offerIngestionJob(jobRepository, mockStep);

        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("offerIngestionJob");
    }

    @Test
    void shouldCreateIngestionStepWithDefaultChunkSize() {
        org.springframework.test.util.ReflectionTestUtils.setField(ingestionBatchConfig, "chunkSize", 10);
        org.springframework.test.util.ReflectionTestUtils.setField(ingestionBatchConfig, "skipLimit", 100);

        Step step = ingestionBatchConfig.ingestionStep(jobRepository, transactionManager, offerReader);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("ingestionStep");
    }

    @Test
    void shouldCreateIngestionStepWithCustomChunkSize() {
        org.springframework.test.util.ReflectionTestUtils.setField(ingestionBatchConfig, "chunkSize", 50);
        org.springframework.test.util.ReflectionTestUtils.setField(ingestionBatchConfig, "skipLimit", 200);

        Step step = ingestionBatchConfig.ingestionStep(jobRepository, transactionManager, offerReader);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("ingestionStep");
    }

    @Test
    void shouldVerifyOfferItemWriterIsInjected() {
        assertThat(ingestionBatchConfig).isNotNull();
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(ingestionBatchConfig, "offerItemWriter"))
                .isEqualTo(offerItemWriter);
    }
}
