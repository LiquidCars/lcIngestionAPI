package net.liquidcars.ingestion.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDto {

    private UUID id;
    private UUID channelGroupId;
    private UUID participantId;
    private String name;
    private boolean enabled;
    private Integer jurisdictionId;
    private String notificationsLink;
    private OffsetDateTime createdAt;
}