package net.liquidcars.ingestion.infra.input.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.infra.input.rest.mapper.IngestionControllerMapper;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class IngestionController implements IngestionApi {

    private final IOfferIngestionProcessService offerIngestionProcessService;
    private final IngestionControllerMapper ingestionControllerMapper;


    @Override
    public ResponseEntity<Void> ingestBatch(List<OfferRequest> offerRequest) {
        offerIngestionProcessService.processOffers(ingestionControllerMapper.toOfferDtoList(offerRequest));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> ingestFromUrl(String format, URI url) {
        offerIngestionProcessService.processOffersFromUrl(format, url);
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> ingestStream(String format, org.springframework.core.io.Resource body) {
        ServletWebRequest servletContainerRequest = (ServletWebRequest) RequestContextHolder.getRequestAttributes();
        assert servletContainerRequest != null;
        HttpServletRequest nativeRequest = servletContainerRequest.getRequest();

        try {
            InputStream inputStream = nativeRequest.getInputStream();
            offerIngestionProcessService.processOffersStream(format, inputStream);
        } catch (IOException e) {
            log.error("Failed to retrieve input stream from request", e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.accepted().build();
    }
}
