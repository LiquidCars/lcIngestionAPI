package net.liquidcars.ingestion.infra.input.rest;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.SortDirection;
import net.liquidcars.ingestion.domain.model.batch.*;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.model.security.AccessRoleEnum;
import net.liquidcars.ingestion.domain.model.security.LCContext;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import net.liquidcars.ingestion.infra.input.rest.mapper.IngestionControllerMapper;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionPayload;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionReport;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionReportPage;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class IngestionController implements IngestionApi {

    private final IOfferIngestionProcessService offerIngestionProcessService;
    private final IngestionControllerMapper ingestionControllerMapper;
    private final IContextService contextService;

    // --- Ingestion Endpoints ---

    @RolesAllowed({AccessRoleEnum.LCSupport_role, AccessRoleEnum.M2M_role})
    @Override
    public ResponseEntity<Void> ingestBatch(
            UUID inventoryId,
            IngestionDumpType dumpType,
            IngestionPayload ingestionPayload,
            String externalPublicationId
    ) {
        log.info("REST: IngestBatch - Inventory: {}, Dump: {}, ExtId: {}", inventoryId, dumpType, externalPublicationId);
        UUID participantId = getParticipantIdFromContext();

        offerIngestionProcessService.processOffers(
                ingestionControllerMapper.toIngestionPayloadDto(ingestionPayload, participantId, inventoryId),
                inventoryId,
                participantId,
                dumpType,
                externalPublicationId
        );

        return ResponseEntity.accepted().build();
    }

    @RolesAllowed({AccessRoleEnum.LCSupport_role, AccessRoleEnum.M2M_role})
    @Override
    public ResponseEntity<Void> ingestFromUrl(
            IngestionFormat format,
            URI url,
            UUID inventoryId,
            IngestionDumpType dumpType,
            OffsetDateTime publicationDate,
            String externalPublicationId
    ) {
        log.info("REST: IngestFromUrl - Format: {}, URL: {}, Inventory: {}", format, url, inventoryId);

        offerIngestionProcessService.processOffersFromUrl(
                format,
                url,
                inventoryId,
                getParticipantIdFromContext(),
                dumpType,
                publicationDate,
                externalPublicationId
        );

        return ResponseEntity.accepted().build();
    }

    @RolesAllowed({AccessRoleEnum.LCSupport_role, AccessRoleEnum.M2M_role})
    @Override
    public ResponseEntity<Void> ingestStream(
            IngestionFormat format,
            UUID inventoryId,
            IngestionDumpType dumpType,
            Resource body,
            OffsetDateTime publicationDate,
            String externalPublicationId
    ) {
        log.info("REST: IngestStream - Format: {}, Inventory: {}, Dump: {}", format, inventoryId, dumpType);

        /*try (InputStream inputStream = body.getInputStream()) {
            offerIngestionProcessService.processOffersStream(
                    format,
                    inputStream,
                    inventoryId,
                    getParticipantIdFromContext(),
                    dumpType,
                    publicationDate,
                    externalPublicationId
            );
        } catch (IOException e) {
            log.error("Failed to extract input stream from body resource", e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message("The binary stream could not be opened")
                    .build();
        }

        return ResponseEntity.accepted().build();*/


        try {
            PipedOutputStream pipedOut = new PipedOutputStream();
            PipedInputStream pipedIn = new PipedInputStream(pipedOut, 8 * 1024 * 1024);

            // Productort: lee del HTTP request y escribe en el pipe
            // Este hilo es independiente del ciclo de vida del HTTP request
            Thread.ofVirtual().start(() -> {
                try (InputStream inputStream = body.getInputStream()) {
                    inputStream.transferTo(pipedOut);
                } catch (Exception e) {
                    log.error("Error transferring HTTP stream to pipe", e);
                } finally {
                    try { pipedOut.close(); } catch (Exception ignored) {}
                }
            });

            // Lanzamos el procesado con el pipe como fuente, ya desacoplado del request
            offerIngestionProcessService.processOffersStream(
                    format,
                    pipedIn,
                    inventoryId,
                    getParticipantIdFromContext(),
                    dumpType,
                    publicationDate,
                    externalPublicationId
            );

        } catch (Exception e) {
            log.error("Failed to initialize stream pipe", e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message("The binary stream could not be opened")
                    .build();
        }

        return ResponseEntity.accepted().build();
    }

    // --- Management Endpoints ---
    @RolesAllowed({AccessRoleEnum.LCSupport_role, AccessRoleEnum.M2M_role})
    @Override
    public ResponseEntity<IngestionReportPage> findIngestionReports(
            Integer page,
            Integer size,
            IngestionReportSortField sortBy,
            SortDirection sortDirection,
            IngestionProcessType processType,
            UUID requesterParticipantId,
            UUID inventoryId,
            String externalRequestId,
            IngestionBatchStatus status,
            IngestionDumpType dumpType,
            Boolean processed,
            Boolean promoted,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            OffsetDateTime updatedFrom,
            OffsetDateTime updatedTo
    ){

        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .processType(processType)
                .requesterParticipantId(requesterParticipantId)
                .inventoryId(inventoryId)
                .externalRequestId(externalRequestId)
                .status(status)
                .dumpType(dumpType)
                .processed(processed)
                .promoted(promoted)
                .createdFrom(createdFrom)
                .createdTo(createdTo)
                .updatedFrom(updatedFrom)
                .updatedTo(updatedTo)
                .build();
        log.info("REST: FindIngestionReports - Filtering request for filter {}", filter);
        IngestionReportPageDto ingestionReportPageDto = offerIngestionProcessService.findIngestionReports(filter);
        return ResponseEntity.ok(ingestionControllerMapper.toIngestionReportPage(ingestionReportPageDto));
    }

    @RolesAllowed({AccessRoleEnum.LCSupport_role, AccessRoleEnum.M2M_role})
    @Override
    public ResponseEntity<IngestionReport> findIngestionReportById(UUID ingestionReportId) {
        log.info("REST: FindIngestionReportById - Job: {}", ingestionReportId);
        IngestionReportDto ingestionReport = offerIngestionProcessService.findIngestionReportById(ingestionReportId);
        return ResponseEntity.ok(ingestionControllerMapper.toIngestionReport(ingestionReport));
    }

    @RolesAllowed({AccessRoleEnum.LCSupport_role})
    @Override
    public ResponseEntity<Void> promoteDraftOffers(UUID ingestionReportId) {
        log.info("REST: PromoteDraftOffers - Job: {}", ingestionReportId);
        offerIngestionProcessService.promoteDraftOffersToVehicleOffers(ingestionReportId, false);
        return ResponseEntity.ok().build();
    }

    @RolesAllowed({AccessRoleEnum.LCSupport_role})
    @Override
    public ResponseEntity<Void> deleteDraftOffers(UUID ingestionReportId) {
        log.info("REST: DeleteDraftOffers - Job: {}", ingestionReportId);
        offerIngestionProcessService.deleteDraftOffersByIngestionReportId(ingestionReportId, false);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extract the participant ID (Vendor/Dealer) from the security context (JWT/Header).
     */
    private UUID getParticipantIdFromContext() {
        LCContext context = contextService.getContext();
        return (context != null) ? UUID.fromString(context.getParticipantId()) : null;
    }
}