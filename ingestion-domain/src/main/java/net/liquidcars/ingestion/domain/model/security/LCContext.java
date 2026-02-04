package net.liquidcars.ingestion.domain.model.security;

import lombok.Data;

import java.util.List;

@Data
public class LCContext {
    private String participantId;
    private String participantType;
    private String language;
    private List<String> roles;
    private String rawToken;
    private String name;
}