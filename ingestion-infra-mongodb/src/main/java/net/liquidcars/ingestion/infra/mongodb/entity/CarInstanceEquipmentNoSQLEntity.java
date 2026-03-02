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
public class CarInstanceEquipmentNoSQLEntity {
    @Field("equipment")
    private KeyValueNoSQLEntity equipment;

    @Field("category")
    private KeyValueNoSQLEntity category;

    @Field("type")
    private KeyValueNoSQLEntity type;

    @Field("description")
    private String description;

    @Field("code")
    private String code;

    @Field("price")
    private MoneyNoSQLEntity price;
}
