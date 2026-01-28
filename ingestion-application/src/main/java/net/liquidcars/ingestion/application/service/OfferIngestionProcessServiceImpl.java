package net.liquidcars.ingestion.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Application service for offer ingestion orchestration.
 * Coordinates between domain logic and infrastructure adapters.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferIngestionProcessServiceImpl implements IOfferIngestionProcessService {

    private final List<IOfferParserService> parsers;
    private final IOfferInfraKafkaProducerService offerInfraKafkaProducerService;

    @Override
    public void processOffers(List<OfferDto> offers) {
        offers.forEach(this::processOffer);
    }

    @Override
    public void processOffersFromUrl(String format, URI url) {
        log.info("Triggering remote ingestion from URL: {} with format: {}", url, format);

        Thread.ofVirtual().start(() -> {
            try (var httpClient = java.net.http.HttpClient.newHttpClient()) {
                var request = java.net.http.HttpRequest.newBuilder()
                        .uri(url)
                        .GET()
                        .build();

                var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    this.processOffersStream(format, response.body());
                } else {
                    log.error("Failed to download file from URL: {}. Status code: {}", url, response.statusCode());
                }
            } catch (Exception e) {
                log.error("Error during remote URL ingestion from {}", url, e);
            }
        });
    }

    @Override
    public void processOffersStream(String format, InputStream inputStream) {
        log.info("Process ingestion from InputStream with format: {}", format);

        IOfferParserService parser = parsers.stream()
                .filter(p -> p.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported format: " + format));

        Thread.ofVirtual().start(() -> {
            try (inputStream) {
                log.info("Starting ingestion for format: {}", format);
                parser.parseAndProcess(inputStream, offer -> {
                    if (offer.isValid()) {
                        processOffer(offer);
                    } else {
                        log.warn("Invalid offer skipped: {}", offer.getExternalId());
                    }
                });

                log.info("Ingestion completed for format: {}", format);
            } catch (Exception e) {
                log.error("Error processing stream", e);
            }
        });
    }

    private void processOffer(OfferDto offerDto){
        offerInfraKafkaProducerService.sendOffer(offerDto);
    }
}
