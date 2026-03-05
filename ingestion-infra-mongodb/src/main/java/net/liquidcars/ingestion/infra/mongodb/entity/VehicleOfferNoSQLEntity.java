package net.liquidcars.ingestion.infra.mongodb.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vehicleoffers")
@CompoundIndex(
        name = "idx_vehicle_offers_composite",
        def = "{ 'owner_reference': 1, 'dealer_reference': 1, 'channel_reference': 1 }"
)
public class VehicleOfferNoSQLEntity extends OfferNoSQLEntity{

    @Id
    private UUID id;

    @Field("agreements")
    private List<AgreementNoSQLEntity> agreements;

    @Field("tiny_locators")
    private List<String> tinyLocators;

}

