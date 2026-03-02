package net.liquidcars.ingestion.infra.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantAddressNoSQLEntity {
    @Field("type")
    private AddressTypeNoSQLEntity type;

    @Field("address")
    private PostalAddressNoSQLEntity address;

}
