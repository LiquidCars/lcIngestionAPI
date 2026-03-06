package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionParserException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionBatchConfigTest  {

    @Mock private OfferItemWriter offerItemWriter;
    @Mock private JobRepository jobRepository;
    @Mock private IngestionSkipListener ingestionSkipListener;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private OfferStreamItemReader offerReader;
    @Mock private JobFailedIdsCollector failedIdsCollector;
    @Mock private JobCompletionNotificationListener jobCompletionListener;

    @InjectMocks
    private IngestionBatchConfig ingestionBatchConfig;

    private SkipPolicy skipPolicy;
    private static final int SKIP_LIMIT = 100;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ingestionBatchConfig, "chunkSize", 10);
        ReflectionTestUtils.setField(ingestionBatchConfig, "skipLimit", SKIP_LIMIT);

        Step step = ingestionBatchConfig.ingestionStep(
                jobRepository, transactionManager, offerReader,
                offerItemWriter, ingestionSkipListener, failedIdsCollector
        );
        skipPolicy = extractSkipPolicy(step);
    }


    @Test
    void shouldCollectFailedIdentifier_whenLCIngestionParserExceptionWithIdentifier() throws Exception {
        ExternalIdInfoDto failedId = TestDataFactory.createExternalIdInfo();
        LCIngestionParserException ex = new LCIngestionParserException(
                LCTechCauseEnum.CONVERSION_ERROR, "parse error", null, failedId
        );

        boolean result = skipPolicy.shouldSkip(ex, 1);

        verify(failedIdsCollector).addId(failedId);
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotCollectId_whenLCIngestionParserExceptionWithNullIdentifier() throws Exception {
        LCIngestionParserException ex = new LCIngestionParserException(
                LCTechCauseEnum.CONVERSION_ERROR, "parse error", null, null
        );

        boolean result = skipPolicy.shouldSkip(ex, 1);

        verify(failedIdsCollector, never()).addId(any());
        assertThat(result).isTrue();
    }


    @Test
    void shouldReturnFalse_whenSkipCountExceedsSkipLimit() throws Exception {
        ExternalIdInfoDto failedId = TestDataFactory.createExternalIdInfo();
        LCIngestionParserException ex = new LCIngestionParserException(
                LCTechCauseEnum.CONVERSION_ERROR, "parse error", null, failedId
        );

        boolean result = skipPolicy.shouldSkip(ex, SKIP_LIMIT + 1);

        assertThat(result).isFalse();
    }

    @Test
    void shouldNotReturnFalse_whenSkipCountEqualsSkipLimit() throws Exception {
        LCIngestionParserException ex = new LCIngestionParserException(
                LCTechCauseEnum.CONVERSION_ERROR, "parse error", null, null
        );

        boolean result = skipPolicy.shouldSkip(ex, SKIP_LIMIT);

        assertThat(result).isTrue();
    }


    @Test
    void shouldReturnTrue_whenLCIngestionExceptionIsConversionError() throws Exception {
        LCIngestionException ex = mockIngestionException(LCTechCauseEnum.CONVERSION_ERROR);

        boolean result = skipPolicy.shouldSkip(ex, 1);

        assertThat(result).isTrue();
    }


    @Test
    void shouldReturnFalse_whenLCIngestionExceptionIsDatabaseError() throws Exception {
        LCIngestionException ex = mockIngestionException(LCTechCauseEnum.DATABASE);

        boolean result = skipPolicy.shouldSkip(ex, 1);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalse_whenLCIngestionExceptionIsInternalError() throws Exception {
        LCIngestionException ex = mockIngestionException(LCTechCauseEnum.INTERNAL_ERROR);

        boolean result = skipPolicy.shouldSkip(ex, 1);

        assertThat(result).isFalse();
    }


    @Test
    void shouldReturnFalse_whenUnexpectedExceptionType() throws Exception {
        RuntimeException ex = new RuntimeException("Unexpected error");

        boolean result = skipPolicy.shouldSkip(ex, 1);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalse_whenNullPointerException() throws Exception {
        NullPointerException ex = new NullPointerException("NPE");

        boolean result = skipPolicy.shouldSkip(ex, 1);

        assertThat(result).isFalse();
    }

    @Test
    void shouldCreateSyncJobLauncher() throws Exception {
        JobLauncher launcher = ingestionBatchConfig.jobLauncher(jobRepository);

        assertThat(launcher).isNotNull();
        assertThat(launcher).isInstanceOf(TaskExecutorJobLauncher.class);
    }

    @Test
    void shouldCreateOfferIngestionJob() {
        Step mockStep = mock(Step.class);

        Job job = ingestionBatchConfig.offerIngestionJob(jobRepository, mockStep, jobCompletionListener);

        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("offerIngestionJob");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private LCIngestionException mockIngestionException(LCTechCauseEnum cause) {
        return new LCIngestionException(cause, "test error", null);
    }

    private SkipPolicy extractSkipPolicy(Step step) {
        try {
            Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
            Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
            return (SkipPolicy) ReflectionTestUtils.getField(chunkProvider, "skipPolicy");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo extraer el SkipPolicy del Step: " + e.getMessage(), e);
        }
    }
}