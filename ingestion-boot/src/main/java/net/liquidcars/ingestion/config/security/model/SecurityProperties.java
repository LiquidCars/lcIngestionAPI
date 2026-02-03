package net.liquidcars.ingestion.config.security.model;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import net.liquidcars.ingestion.domain.model.security.SecurityProfilesEnumDto;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private String securityProfileIssuer;
    private String allowedOriginsPattern = "*";
    private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
    private String allowedHeaders = "Authorization,Content-Type";
    private String exposedHeaders = "Authorization";
    private Boolean allowCredentials = true;
    private Long maxAgeSeconds = 3600L;

    // Paths that will be restricted by size
    private List<String> sizeRestrictedPaths = new ArrayList<>();
    // Default limit using Spring's DataSize
    private DataSize maxBatchSize = DataSize.ofMegabytes(10);

    private List<String> issuers = new ArrayList<>();

    @PostConstruct
    public void initIssuers() {
        if (securityProfileIssuer != null) {
            this.issuers = Arrays.stream(SecurityProfilesEnumDto.values())
                    .map(profile -> securityProfileIssuer + profile.toString())
                    .toList();
        }
    }
}
