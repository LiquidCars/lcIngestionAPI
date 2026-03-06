package net.liquidcars.ingestion.infra.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TinyLocatorNoSQLEntity {

    @Field("tiny_locator_id")
    private String tinyLocatorId;

    @Field("offer_id")
    private UUID offerId;

    @Field("inventory_id")
    private UUID inventoryId;

    @Field("agreement_id")
    private UUID agreementId;

    @Field("channel_id")
    private UUID channelId;

    @Field("vehicle_seller_id")
    private UUID vehicleShellerId;
}