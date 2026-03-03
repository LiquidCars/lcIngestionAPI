package net.liquidcars.ingestion.application.service.scheduler;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PromotionSchedulerTest {

    @Mock
    private IOfferIngestionProcessService offerIngestionProcessService;

    @InjectMocks
    private PromotionScheduler promotionScheduler;

    @Test
    @DisplayName("Debe llamar al servicio de promoción de ofertas diferidas una vez")
    void shouldCallServiceWhenExecuted() {
        promotionScheduler.executeDeferredPromotions();

        verify(offerIngestionProcessService, times(1)).executeDeferredPromotions();
    }
}
