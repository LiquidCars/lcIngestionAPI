package net.liquidcars.ingestion.domain.model.security;

import lombok.Data;

import java.util.List;

@Data
public class LCContext {
    private String participantId;
    private String rguId;
    private String participantType;
    private String language;
    private List<String> roles;
    private String rawToken;
    private String name;
    private String givenName;
    private String preferredUserName;
    private String familyName;
    private String email;

}