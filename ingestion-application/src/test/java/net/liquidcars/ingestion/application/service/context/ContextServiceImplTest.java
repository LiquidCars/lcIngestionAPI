package net.liquidcars.ingestion.application.service.context;

import net.liquidcars.ingestion.domain.model.security.LCContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextServiceImplTest {

    private ContextServiceImpl contextService;

    @BeforeEach
    void setUp() {
        contextService = new ContextServiceImpl();
        // Limpiamos antes de cada test por si acaso el hilo de JUnit se reutiliza
        contextService.clear();
    }

    @Test
    @DisplayName("Debe retornar contexto de SYSTEM si el ThreadLocal está vacío")
    void getContext_ShouldReturnSystemContext_WhenEmpty() {
        // Act
        LCContext ctx = contextService.getContext();

        // Assert
        assertThat(ctx).isNotNull();
        assertThat(ctx.getName()).isEqualTo("SYSTEM_SCHEDULER");
        assertThat(ctx.getParticipantId()).isEqualTo("SYSTEM");
        assertThat(ctx.getRoles()).containsExactly("LCAdmin");
    }

    @Test
    @DisplayName("Debe permitir establecer y recuperar un contexto personalizado")
    void setAndGetContext_ShouldWorkCorrectly() {
        // Arrange
        LCContext customCtx = new LCContext();
        customCtx.setName("USER_TEST");
        customCtx.setParticipantId("PART-123");

        // Act
        contextService.setContext(customCtx);
        LCContext result = contextService.getContext();

        // Assert
        assertThat(result).isSameAs(customCtx);
        assertThat(result.getName()).isEqualTo("USER_TEST");
    }

    @Test
    @DisplayName("clear() debe eliminar el contexto del hilo actual")
    void clear_ShouldRemoveContext() {
        // Arrange
        contextService.getContext(); // Esto crea el System Context

        // Act
        contextService.clear();

        // Al pedirlo de nuevo, debería crearse uno NUEVO (por la lógica de tu if == null)
        LCContext afterClear = contextService.getContext();

        assertThat(afterClear).isNotNull();
        assertThat(afterClear.getName()).isEqualTo("SYSTEM_SCHEDULER");
    }

    @Test
    @DisplayName("Debe garantizar que el contexto esté aislado entre diferentes hilos")
    void context_ShouldBeIsolatedPerThread() throws ExecutionException, InterruptedException {
        // 1. Establecemos un contexto en el hilo principal
        LCContext mainThreadCtx = new LCContext();
        mainThreadCtx.setName("MAIN_THREAD");
        contextService.setContext(mainThreadCtx);

        // 2. Ejecutamos en otro hilo y verificamos que no vea el del principal
        CompletableFuture<LCContext> otherThreadTask = CompletableFuture.supplyAsync(() -> {
            // Este hilo debería obtener el SYSTEM context al estar vacío para él
            return contextService.getContext();
        });

        LCContext otherThreadResult = otherThreadTask.get();

        // Assert
        assertThat(otherThreadResult.getName()).isEqualTo("SYSTEM_SCHEDULER");
        assertThat(contextService.getContext().getName()).isEqualTo("MAIN_THREAD");
    }
}
