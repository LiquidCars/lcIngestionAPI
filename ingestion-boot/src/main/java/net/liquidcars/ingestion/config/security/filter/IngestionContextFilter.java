package net.liquidcars.ingestion.config.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.domain.model.security.LCContext;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class IngestionContextFilter extends OncePerRequestFilter {

    private final IContextService contextService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            LCContext ctx = getLcContextFromToken(jwt);

            // Set the context for the current request
            contextService.setContext(ctx);
        }

        chain.doFilter(request, response);
    }

    private static LCContext getLcContextFromToken(Jwt jwt) {
        LCContext ctx = new LCContext();

        // Mapping values directly from JWT Claims
        ctx.setRawToken(jwt.getTokenValue());
        ctx.setParticipantId(jwt.getClaimAsString("participant_id"));
        ctx.setParticipantType(jwt.getClaimAsStringList("participant_type") != null ?
                jwt.getClaimAsStringList("participant_type").get(0) : null);
        ctx.setLanguage(jwt.getClaimAsString("participant_default_language"));
        ctx.setName(jwt.getClaimAsString("name"));

        // Extract roles from the "roles" claim in token
        ctx.setRoles(jwt.getClaimAsStringList("roles"));
        return ctx;
    }
}