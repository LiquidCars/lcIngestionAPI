package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class JobFailedIdsCollectorTest {

    @Spy
    private JobFailedIdsCollector collector;

    @BeforeEach
    void setUp() {
        collector = new JobFailedIdsCollector();
    }

    @Test
    @DisplayName("Debería añadir IDs y devolverlos todos")
    void shouldAddAndReturnIds() {
        // Given
        ExternalIdInfoDto id1 = TestDataFactory.createExternalIdInfoFull("OWN-1", "DLR-1", "CH-1");
        ExternalIdInfoDto id2 = TestDataFactory.createExternalIdInfoFull("OWN-2", "DLR-2", "CH-2");

        // When
        collector.addId(id1);
        collector.addId(id2);
        List<ExternalIdInfoDto> result = collector.getFailedIds();

        // Then
        assertThat(result).hasSize(2).containsExactlyInAnyOrder(id1, id2);
    }

    @Test
    @DisplayName("No debería permitir duplicados (basado en equals de ExternalIdInfoDto)")
    void shouldNotStoreDuplicateIds() {
        // Given
        ExternalIdInfoDto id1 = TestDataFactory.createExternalIdInfo();
        ExternalIdInfoDto id2 = id1;

        // When
        collector.addId(id1);
        collector.addId(id2);

        // Then
        assertThat(collector.getFailedIds()).hasSize(1);
    }

    @Test
    @DisplayName("Debería limpiar la lista correctamente y asegurar cobertura")
    void shouldClearCollector() {
        // Given: metemos datos
        collector.addId(TestDataFactory.createExternalIdInfoFull("A", "B", "C"));
        assertThat(collector.getFailedIds()).isNotEmpty();

        // When: llamamos al método
        collector.clear();

        // Then: el estado final debe ser vacío
        // Esto por sí solo ya da el 100% de cobertura porque el código se ejecuta
        assertThat(collector.getFailedIds()).isEmpty();
    }

}