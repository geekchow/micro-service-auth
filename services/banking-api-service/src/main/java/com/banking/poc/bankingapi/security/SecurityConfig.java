package com.banking.poc.bankingapi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class SecurityConfig {

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter(JwtAuthenticationDecoder jwtAuthenticationDecoder) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JwtAuthenticationFilter(jwtAuthenticationDecoder));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    JwtAuthenticationDecoder jwtAuthenticationDecoder(ObjectMapper objectMapper) {
        return token -> {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !StringUtils.hasText(parts[1])) {
                throw new InvalidJwtException("Token must be a JWT");
            }

            try {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                JsonNode claims = objectMapper.readTree(payload);
                String subject = claims.path("sub").asText();
                if (!StringUtils.hasText(subject)) {
                    throw new InvalidJwtException("JWT subject is required");
                }
                return new JwtClaims(subject);
            } catch (IOException | IllegalArgumentException ex) {
                throw new InvalidJwtException("Invalid JWT", ex);
            }
        };
    }
}

interface JwtAuthenticationDecoder {
    JwtClaims decode(String token);
}

record JwtClaims(String subject) {
}

class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationDecoder jwtAuthenticationDecoder;

    JwtAuthenticationFilter(JwtAuthenticationDecoder jwtAuthenticationDecoder) {
        this.jwtAuthenticationDecoder = jwtAuthenticationDecoder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return !requestUri.startsWith("/api/") || "/actuator/health".equals(requestUri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = authorization.substring(7);
        try {
            request.setAttribute("jwtClaims", jwtAuthenticationDecoder.decode(token));
            filterChain.doFilter(request, response);
        } catch (InvalidJwtException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}

class InvalidJwtException extends RuntimeException {

    InvalidJwtException(String message) {
        super(message);
    }

    InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
