package net.liquidcars.ingestion.domain.service.utils;

import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OfferUtils {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

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

    public static String generateTinyLocator(UUID offer, UUID inventory, UUID agreement, UUID ocp, UUID vsel) {
        try {
            String seed = Stream.of(offer, inventory, agreement, ocp, vsel)
                    .map(id -> id == null ? "" : id.toString())
                    .collect(Collectors.joining());

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(seed.getBytes(StandardCharsets.UTF_8));

            long hashValue = 0;
            for (int i = 0; i < 8; i++) {
                hashValue = (hashValue << 8) | (hashBytes[i] & 0xFF);
            }

            return encodeBase62(Math.abs(hashValue)).substring(0, 8);

        } catch (NoSuchAlgorithmException e) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INTERNAL_ERROR)
                    .message("Error SHA-256 algorithm not found")
                    .cause(e)
                    .build();
        }
    }

    private static String encodeBase62(long n) {
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            sb.append(ALPHABET.charAt((int) (n % 62)));
            n /= 62;
        }
        while (sb.length() < 8) {
            sb.append('0');
        }
        return sb.reverse().toString();
    }
}
