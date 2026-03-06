package net.liquidcars.ingestion.domain.model;

import lombok.Data;

import java.util.UUID;

@Data
public class VehicleSellerDto {

    private UUID id;
    private String name;
    private boolean enabled;
}