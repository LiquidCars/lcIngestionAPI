package net.liquidcars.ingestion.infra.postgresql.specification;

import jakarta.persistence.criteria.*;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.batch.IngestionProcessType;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportFilterDto;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionReportSpecificationTest {

    @Mock private Root<IngestionReportEntity> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;
    @Mock private Path<Object> path;
    @Mock private Predicate predicate;
    @Mock private Path<OffsetDateTime> datePath;

    @BeforeEach
    void setUp() {
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(cb.equal(any(), any())).thenReturn(predicate);
        lenient().when(cb.greaterThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);
        lenient().when(cb.lessThanOrEqualTo(any(), any(Comparable.class))).thenReturn(predicate);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);
    }

    @Test
    void shouldReturnEmptyAndPredicateWhenFilterIsEmpty() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder().build();

        Specification<IngestionReportEntity> spec = IngestionReportSpecification.filterBy(filter);
        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        verify(cb).and(new Predicate[0]);
        verify(cb, never()).equal(any(), any());
    }

    @Test
    void shouldAddProcessTypePredicateWhenPresent() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .processType(IngestionProcessType.PROCESS)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("processType");
        verify(cb).equal(path, IngestionProcessType.PROCESS);
    }

    @Test
    void shouldAddRequesterParticipantIdPredicateWhenPresent() {
        UUID participantId = UUID.randomUUID();
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .requesterParticipantId(participantId)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("requesterParticipantId");
        verify(cb).equal(path, participantId);
    }

    @Test
    void shouldAddInventoryIdPredicateWhenPresent() {
        UUID inventoryId = UUID.randomUUID();
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .inventoryId(inventoryId)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("inventoryId");
        verify(cb).equal(path, inventoryId);
    }

    @Test
    void shouldAddExternalRequestIdPredicateWhenPresent() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .externalRequestId("EXT-001")
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("externalRequestId");
        verify(cb).equal(path, "EXT-001");
    }

    @Test
    void shouldNotAddExternalRequestIdPredicateWhenBlank() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .externalRequestId("   ")
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root, never()).get("externalRequestId");
    }

    @Test
    void shouldNotAddExternalRequestIdPredicateWhenEmpty() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .externalRequestId("")
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root, never()).get("externalRequestId");
    }

    @Test
    void shouldAddStatusPredicateWhenPresent() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .status(IngestionBatchStatus.STARTING)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("status");
        verify(cb).equal(path, IngestionBatchStatus.STARTING);
    }

    @Test
    void shouldAddDumpTypePredicateWhenPresent() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .dumpType(IngestionDumpType.UPDATE)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("dumpType");
        verify(cb).equal(path, IngestionDumpType.UPDATE);
    }

    @Test
    void shouldAddProcessedPredicateWhenPresent() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .processed(true)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("processed");
        verify(cb).equal(path, true);
    }

    @Test
    void shouldAddPromotedPredicateWhenPresent() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .promoted(false)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("promoted");
        verify(cb).equal(path, false);
    }

    @Test
    void shouldAddCreatedFromPredicateWhenPresent() {
        OffsetDateTime from = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .createdFrom(from)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("createdAt");
        verify(cb).<OffsetDateTime>greaterThanOrEqualTo(any(), eq(from));
    }

    @Test
    void shouldAddCreatedToPredicateWhenPresent() {
        OffsetDateTime to = OffsetDateTime.of(2024, 12, 31, 23, 59, 0, 0, ZoneOffset.UTC);
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .createdTo(to)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("createdAt");
        verify(cb).<OffsetDateTime>lessThanOrEqualTo(any(), eq(to));
    }

    @Test
    void shouldAddBothCreatedFromAndToPredicatesWhenPresent() {
        OffsetDateTime from = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime to   = OffsetDateTime.of(2024, 12, 31, 23, 59, 0, 0, ZoneOffset.UTC);
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .createdFrom(from)
                .createdTo(to)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(cb).<OffsetDateTime>greaterThanOrEqualTo(any(), eq(from));
        verify(cb).<OffsetDateTime>lessThanOrEqualTo(any(), eq(to));
    }

    @Test
    void shouldAddUpdatedFromPredicateWhenPresent() {
        OffsetDateTime from = OffsetDateTime.of(2024, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .updatedFrom(from)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("updatedAt");
        verify(cb).<OffsetDateTime>greaterThanOrEqualTo(any(), eq(from));
    }

    @Test
    void shouldAddUpdatedToPredicateWhenPresent() {
        OffsetDateTime to = OffsetDateTime.of(2024, 6, 30, 23, 59, 0, 0, ZoneOffset.UTC);
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .updatedTo(to)
                .build();

        IngestionReportSpecification.filterBy(filter).toPredicate(root, query, cb);

        verify(root).get("updatedAt");
        verify(cb).<OffsetDateTime>lessThanOrEqualTo(any(), eq(to));
    }

}
