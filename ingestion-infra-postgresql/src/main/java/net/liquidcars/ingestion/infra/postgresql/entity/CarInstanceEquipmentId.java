package net.liquidcars.ingestion.infra.postgresql.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarInstanceEquipmentId {
    private Integer id;
    private Long vehicleInstance;
}
