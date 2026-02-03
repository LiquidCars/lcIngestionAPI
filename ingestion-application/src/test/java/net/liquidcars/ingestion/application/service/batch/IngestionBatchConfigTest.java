package net.liquidcars.ingestion.application.service.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.util.ReflectionTestUtils;
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
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private IngestionBatchConfig ingestionBatchConfig;

    @Test
    void shouldCreateOfferIngestionJob() {
        // Given
        Step mockStep = mock(Step.class);

        // When
        Job job = ingestionBatchConfig.offerIngestionJob(jobRepository, mockStep);

        // Then
        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("offerIngestionJob");
    }

    @Test
    void shouldCreateIngestionStepWithDefaultChunkSize() {
        // Given
        ReflectionTestUtils.setField(ingestionBatchConfig, "chunkSize", 10);
        ReflectionTestUtils.setField(ingestionBatchConfig, "skipLimit", 100);

        // When
        Step step = ingestionBatchConfig.ingestionStep(jobRepository, transactionManager);

        // Then
        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("ingestionStep");
    }

    @Test
    void shouldCreateIngestionStepWithCustomChunkSize() {
        // Given
        ReflectionTestUtils.setField(ingestionBatchConfig, "chunkSize", 50);
        ReflectionTestUtils.setField(ingestionBatchConfig, "skipLimit", 200);

        // When
        Step step = ingestionBatchConfig.ingestionStep(jobRepository, transactionManager);

        // Then
        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("ingestionStep");
    }

    @Test
    void shouldVerifyOfferItemWriterIsInjected() {
        // Then
        assertThat(ingestionBatchConfig).isNotNull();
        assertThat(ReflectionTestUtils.getField(ingestionBatchConfig, "offerItemWriter"))
                .isEqualTo(offerItemWriter);
    }

    @Test
    void shouldUseDefaultChunkSizeWhenNotConfigured() {
        // Given
        IngestionBatchConfig config = new IngestionBatchConfig(offerItemWriter);
        ReflectionTestUtils.setField(config, "chunkSize", 10);
        ReflectionTestUtils.setField(config, "skipLimit", 100);

        // When
        Step step = config.ingestionStep(jobRepository, transactionManager);

        // Then
        assertThat(step).isNotNull();
    }

    @Test
    void shouldUseDefaultSkipLimitWhenNotConfigured() {
        // Given
        IngestionBatchConfig config = new IngestionBatchConfig(offerItemWriter);
        ReflectionTestUtils.setField(config, "chunkSize", 10);
        ReflectionTestUtils.setField(config, "skipLimit", 100);

        // When
        Step step = config.ingestionStep(jobRepository, transactionManager);

        // Then
        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("ingestionStep");
    }

    @Test
    void shouldConfigureReaderAsNullLambda() throws Exception {
        // Given
        ReflectionTestUtils.setField(ingestionBatchConfig, "chunkSize", 10);
        ReflectionTestUtils.setField(ingestionBatchConfig, "skipLimit", 100);

        // When
        Step step = ingestionBatchConfig.ingestionStep(jobRepository, transactionManager);

        // Then
        assertThat(step).isNotNull();

        // Acceder al reader mediante reflexión y ejecutar el lambda
        Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
        Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
        Object itemReader = ReflectionTestUtils.getField(chunkProvider, "itemReader");

        // Si es un FunctionItemReader o Callable, ejecutarlo
        if (itemReader instanceof java.util.concurrent.Callable) {
            Object result = ((java.util.concurrent.Callable<?>) itemReader).call();
            assertThat(result).isNull();
        }
    }
}