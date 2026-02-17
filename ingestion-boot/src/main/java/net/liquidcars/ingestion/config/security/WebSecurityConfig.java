package net.liquidcars.ingestion.config.security;

import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.config.security.converter.SecurityProfileRoleConverter;
import net.liquidcars.ingestion.config.security.filter.IngestionContextFilter;
import net.liquidcars.ingestion.config.security.model.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final SecurityProperties props;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, IngestionContextFilter contextFilter) throws Exception {

        // Custom Resolver that applies our Role Converter to every issuer
        JwtIssuerAuthenticationManagerResolver authenticationManagerResolver =
                new JwtIssuerAuthenticationManagerResolver(issuer -> {
                    JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuer);
                    JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
                    provider.setJwtAuthenticationConverter(new SecurityProfileRoleConverter());
                    return provider::authenticate;
                });

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/health-check",
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/v3/api-docs.yaml",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/swagger-ui.html/**",
                            "/swagger-resources",
                            "/swagger-resources/**",
                            "/webjars/**",
                            "/favicon.ico",
                            "/error",
                            "/api/ingestion-api.yml"
                    ).permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationManagerResolver(authenticationManagerResolver)
                )
                // Register your context filter after authentication
                .addFilterAfter(contextFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(Arrays.asList(props.getAllowedOriginsPattern().split(",")));
        config.setAllowedMethods(Arrays.asList(props.getAllowedMethods().split(",")));
        config.setAllowedHeaders(Arrays.asList(props.getAllowedHeaders().split(",")));
        config.setExposedHeaders(Arrays.asList(props.getExposedHeaders().split(",")));

        config.setAllowCredentials(props.getAllowCredentials());
        config.setMaxAge(props.getMaxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}