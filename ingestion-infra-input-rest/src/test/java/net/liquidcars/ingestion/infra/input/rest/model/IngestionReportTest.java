package net.liquidcars.ingestion.infra.input.rest.model;

import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class IngestionReportTest {

    private IngestionReport report;

    @BeforeEach
    void setUp() {
        report = new IngestionReport();
    }

    @Test
    @DisplayName("Cobertura 100% de Getters, Setters y Fluent API")
    void fullAccessorsTest() {
        IngestionReport report = new IngestionReport();
        UUID uuid = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        IngestionReport result = report.id(uuid)
                .processType("BATCH")
                .batchJobId(uuid)
                .requesterParticipantId(uuid)
                .inventoryId(uuid)
                .externalRequestId("REQ-123")
                .publicationDate(now)
                .status("COMPLETED")
                .readCount(10)
                .writeCount(8)
                .skipCount(2)
                .processed(true)
                .createdAt(now)
                .updatedAt(now);

        assertAll("Verificación de campos",
                () -> assertThat(result).isSameAs(report),
                () -> assertThat(report.getId()).isEqualTo(uuid),
                () -> assertThat(report.getProcessType()).isEqualTo("BATCH"),
                () -> assertThat(report.getPublicationDate()).isEqualTo(now),
                () -> assertThat(report.getReadCount()).isEqualTo(10),
                () -> assertThat(report.getProcessed()).isTrue()
        );
    }

    @Test
    @DisplayName("Cobertura de ramas (Branches) para métodos addItems")
    void testListMethodsBranches() {
        IngestionReport report = new IngestionReport();
        report.setFailedExternalIds(null);
        report.setIdsForDelete(null);

        report.addFailedExternalIdsItem(new ExternalIdInfo());
        report.addIdsForDeleteItem("ID-DEL");

        assertThat(report.getFailedExternalIds()).hasSize(1);
        assertThat(report.getIdsForDelete()).hasSize(1);

        IngestionReport reportWithData = TestDataFactory.createIngestionReportWithLists(1, 1);
        reportWithData.addFailedExternalIdsItem(new ExternalIdInfo());

        assertThat(reportWithData.getFailedExternalIds()).hasSize(2);
    }

    @Test
    @DisplayName("Cobertura 100% de Equals, HashCode y ToString")
    void testObjectContracts() {
        IngestionReport report1 = TestDataFactory.createRandomIngestionReport();
        IngestionReport report2 = TestDataFactory.createRandomIngestionReport();

        report2.id(report1.getId())
                .processType(report1.getProcessType())
                .batchJobId(report1.getBatchJobId());

        assertAll("Contratos básicos",
                () -> assertThat(report1.equals(report1)).isTrue(),

                () -> assertThat(report1.equals(null)).isFalse(),
                () -> assertThat(report1.equals("not-a-report")).isFalse(),

                () -> assertThat(report1.hashCode()).isNotZero(),
                () -> assertThat(report1.toString()).contains("class IngestionReport")
        );
    }

    @Test
    @DisplayName("Verificación de valores por defecto")
    void testDefaults() {
        IngestionReport report = new IngestionReport();
        assertThat(report.getDumpType()).isEqualTo(net.liquidcars.ingestion.domain.model.batch.IngestionDumpType.INCREMENTAL);
    }

    @Test
    @DisplayName("Cobertura total de Getters y Setters tradicionales")
    void testGettersAndSetters() {
        UUID uuid = UUID.randomUUID();
        OffsetDateTime date = OffsetDateTime.now();
        List<ExternalIdInfo> failedIds = new ArrayList<>();
        List<String> deleteIds = new ArrayList<>();

        report.setId(uuid);
        report.setProcessType("MANUAL");
        report.setBatchJobId(uuid);
        report.setRequesterParticipantId(uuid);
        report.setInventoryId(uuid);
        report.setExternalRequestId("EXT-1");
        report.setPublicationDate(date);
        report.setStatus("OK");
        report.setDumpType(net.liquidcars.ingestion.domain.model.batch.IngestionDumpType.INCREMENTAL);
        report.setReadCount(100);
        report.setWriteCount(90);
        report.setSkipCount(10);
        report.setFailedExternalIds(failedIds);
        report.setIdsForDelete(deleteIds);
        report.setProcessed(true);
        report.setCreatedAt(date);
        report.setUpdatedAt(date);

        assertAll("Getters coverage",
                () -> assertThat(report.getId()).isEqualTo(uuid),
                () -> assertThat(report.getProcessType()).isEqualTo("MANUAL"),
                () -> assertThat(report.getBatchJobId()).isEqualTo(uuid),
                () -> assertThat(report.getRequesterParticipantId()).isEqualTo(uuid),
                () -> assertThat(report.getInventoryId()).isEqualTo(uuid),
                () -> assertThat(report.getExternalRequestId()).isEqualTo("EXT-1"),
                () -> assertThat(report.getPublicationDate()).isEqualTo(date),
                () -> assertThat(report.getStatus()).isEqualTo("OK"),
                () -> assertThat(report.getDumpType()).isEqualTo(net.liquidcars.ingestion.domain.model.batch.IngestionDumpType.INCREMENTAL),
                () -> assertThat(report.getReadCount()).isEqualTo(100),
                () -> assertThat(report.getWriteCount()).isEqualTo(90),
                () -> assertThat(report.getSkipCount()).isEqualTo(10),
                () -> assertThat(report.getFailedExternalIds()).isSameAs(failedIds),
                () -> assertThat(report.getIdsForDelete()).isSameAs(deleteIds),
                () -> assertThat(report.getProcessed()).isTrue(),
                () -> assertThat(report.getCreatedAt()).isEqualTo(date),
                () -> assertThat(report.getUpdatedAt()).isEqualTo(date)
        );
    }

    @Test
    @DisplayName("Cobertura total de métodos Fluent")
    void testFluentMethods() {
        UUID uuid = UUID.randomUUID();
        IngestionReport result = report.id(uuid)
                .processType("AUTO")
                .batchJobId(uuid)
                .idsForDelete(new ArrayList<>());

        assertThat(result).isSameAs(report);
        assertThat(report.getProcessType()).isEqualTo("AUTO");
    }

    @Test
    @DisplayName("Cobertura de todas las ramas del método Equals")
    void testEqualsCoverage() {
        IngestionReport report1 = new IngestionReport();
        report1.setId(UUID.randomUUID());

        IngestionReport report2 = new IngestionReport();
        report2.setId(report1.getId());

        assertAll("Equals branches",
                () -> assertThat(report1.equals(report1)).isTrue(),

                () -> assertThat(report1.equals(null)).isFalse(),

                () -> assertThat(report1.equals(new Object())).isFalse(),

                () -> assertThat(report1.equals(report2)).isTrue(),

                () -> {
                    report2.setId(UUID.randomUUID());
                    assertThat(report1.equals(report2)).isFalse();
                }
        );
    }

    @Test
    @DisplayName("Cobertura de HashCode y ToString")
    void testOtherObjectMethods() {
        report.setId(UUID.randomUUID());

        assertAll("Object methods",
                () -> assertThat(report.hashCode()).isNotZero(),
                () -> assertThat(report.toString()).contains("class IngestionReport"),
                () -> assertThat(report.id(null).toString()).contains("id: null")
        );
    }

    @Test
    @DisplayName("Cobertura de los métodos Fluent restantes: dumpType y failedExternalIds")
    void coverageKillerForRemainingFluentMethods() {
        IngestionDumpType dumpType = IngestionDumpType.INCREMENTAL;

        IngestionReport resultDump = report.dumpType(dumpType);

        assertThat(resultDump).isSameAs(report);
        assertThat(report.getDumpType()).isEqualTo(dumpType);

        List<ExternalIdInfo> newList = new ArrayList<>();
        newList.add(new ExternalIdInfo());

        IngestionReport resultList = report.failedExternalIds(newList);

        assertThat(resultList).isSameAs(report);
        assertThat(report.getFailedExternalIds()).hasSize(1).isEqualTo(newList);
    }
}
