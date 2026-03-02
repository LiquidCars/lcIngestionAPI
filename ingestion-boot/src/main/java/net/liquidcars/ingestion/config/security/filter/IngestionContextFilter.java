package net.liquidcars.ingestion.config.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.config.security.model.SecurityProperties;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.model.security.LCContext;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionContextFilter extends OncePerRequestFilter {

    private final IContextService contextService;
    private final SecurityProperties props;

    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        try {
            populateContext();
            validateRequestSize(request);
            chain.doFilter(request, response);
        } catch (Exception ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        } finally {
            contextService.clear();
        }
    }

    private void validateRequestSize(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean isRestricted = props.getSizeRestrictedPaths().stream().anyMatch(path::contains);

        if (isRestricted && request.getContentLengthLong() > props.getMaxBatchSize().toBytes()) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.PAYLOAD_TOO_LARGE)
                    .message("Payload too large. Limit: " + props.getMaxBatchSize())
                    .build();
        }
    }

    private void populateContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            LCContext ctx = new LCContext();
            ctx.setRawToken(jwt.getTokenValue());
            ctx.setParticipantId(jwt.getClaimAsString("participant_id"));
            ctx.setRguId(jwt.getClaimAsString("rgu_id"));
            ctx.setLanguage(jwt.getClaimAsString("participant_default_language"));
            ctx.setName(jwt.getClaimAsString("name"));
            ctx.setEmail(jwt.getClaimAsString("email"));
            ctx.setGivenName(jwt.getClaimAsString("given_name"));
            ctx.setPreferredUserName(jwt.getClaimAsString("preferred_username"));
            ctx.setFamilyName(jwt.getClaimAsString("family_name"));
            ctx.setRoles(jwt.getClaimAsStringList("roles"));
            contextService.setContext(ctx);
        }
    }
}