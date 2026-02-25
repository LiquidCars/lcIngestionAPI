package net.liquidcars.ingestion.infra.postgresql.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionReportRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionScheduler {

    private final IngestionReportRepository reportRepository;
    private final IOfferInfraNoSQLService offerInfraNoSQLService;

    @Scheduled(cron = "${ingestion.promotionScheduler.cron}")
    @Transactional
    public void executeDeferredPromotions() {
        OffsetDateTime time = OffsetDateTime.now();
        log.info("Checking for reports ready to be promoted. Current time: {}", time);

        List<IngestionReportEntity> pendingReports = reportRepository.findPendingPromotions(time);

        if (pendingReports.isEmpty()) {
            log.info("No pending promotions found.");
            return;
        }

        log.info("Found {} reports to promote.", pendingReports.size());

        for (IngestionReportEntity report : pendingReports) {
            try {
                log.info("Scheduled Promotion starting for report: {} (PublicationDate: {})",
                        report.getId(), report.getPublicationDate());
                List<UUID> bookedIds = report.getActiveBookedOfferIds() != null
                        ? report.getActiveBookedOfferIds()
                        : List.of();

                offerInfraNoSQLService.promoteDraftOffersToVehicleOffers(
                        report.getId(),
                        report.getDumpType(),
                        report.getInventoryId(),
                        report.getIdsForDelete(),
                        bookedIds
                );

                report.setPromoted(true);
                report.setUpdatedAt(OffsetDateTime.now());
                reportRepository.save(report);

                log.info("Report {} successfully promoted.", report.getId());

            } catch (Exception e) {
                log.error("Failed to promote report {}.",
                        report.getId(), e);
            }
        }
    }
}
