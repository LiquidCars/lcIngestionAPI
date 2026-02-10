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
public class CarOfferResourceNoSQLEntity {
    @Field("type")
    private KeyValueNoSQLEntity type;

    @Field("resource")
    private String resource;

    @Field("compressed_resource")
    private byte[] compressedResource;
}
