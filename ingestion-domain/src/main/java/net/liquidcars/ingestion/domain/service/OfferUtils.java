package net.liquidcars.ingestion.domain.service;

import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OfferUtils {

    public static UUID deriveOfferId(String ownerRef, String dealerRef, String channelRef) {
        String seed = buildCompositeKey(ownerRef, dealerRef, channelRef);
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    public static String extractRef(ExternalIdInfoDto info) {
        if (info == null) return buildCompositeKey(null, null, null);
        return buildCompositeKey(info.getOwnerReference(), info.getDealerReference(), info.getChannelReference());
    }

    public static String buildCompositeKey(String owner, String dealer, String channel) {
        return Stream.of(owner, dealer, channel)
                .map(s -> s != null ?  s.trim() : "")
                .collect(Collectors.joining("|"));
    }
}
