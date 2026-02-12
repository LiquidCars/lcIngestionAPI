package net.liquidcars.ingestion.infra.input.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.model.security.AccessRoleEnum;
import net.liquidcars.ingestion.domain.model.security.LCContext;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import net.liquidcars.ingestion.infra.input.rest.mapper.IngestionControllerMapper;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class IngestionController implements IngestionApi {

    private final IOfferIngestionProcessService offerIngestionProcessService;
    private final IngestionControllerMapper ingestionControllerMapper;

    @Autowired
    private IContextService contextService;


    @RolesAllowed({AccessRoleEnum.LCSupport_role, AccessRoleEnum.M2M_role})
    @Override
    public ResponseEntity<Void> ingestBatch(List<OfferRequest> offerRequest) {
        offerIngestionProcessService.processOffers(ingestionControllerMapper.toOfferDtoList(offerRequest, getParticipantIdFromContext()));
        return ResponseEntity.ok().build();
    }

    @RolesAllowed({AccessRoleEnum.LCSupport_role, AccessRoleEnum.M2M_role})
    @Override
    public ResponseEntity<Void> ingestFromUrl(String format, URI url) {
        offerIngestionProcessService.processOffersFromUrl(format, url);
        return ResponseEntity.accepted().build();
    }

    @RolesAllowed({AccessRoleEnum.LCSupport_role, AccessRoleEnum.M2M_role})
    @Override
    public ResponseEntity<Void> ingestStream(String format, org.springframework.core.io.Resource body) {
        try (InputStream inputStream = body.getInputStream()) {
            offerIngestionProcessService.processOffersStream(format, inputStream);
        } catch (IOException e) {
            log.error("Failed to read input stream from Resource", e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message("Error opening input stream from file")
                    .build();
        }
        return ResponseEntity.accepted().build();
    }

    private String getParticipantIdFromContext(){
        LCContext context = contextService.getContext();
        if (context != null && context.getParticipantId() != null) {
            return context.getParticipantId();
        }
        return null;
    }
}
