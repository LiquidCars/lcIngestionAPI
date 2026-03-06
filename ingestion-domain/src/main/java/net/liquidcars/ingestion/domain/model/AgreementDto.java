package net.liquidcars.ingestion.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementDto {

    private UUID id;
    private String name;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private boolean enabled;
    private VehicleSellerDto vehicleSeller;
    private List<ChannelDto> channels;
}