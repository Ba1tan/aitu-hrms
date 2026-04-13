package kz.aitu.hrms.gateway.filter;

import kz.aitu.hrms.common.jwt.JwtTokenValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public final class JwtAuthFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/actuator/health"
    );

    private final JwtTokenValidator jwtTokenValidator;

    public JwtAuthFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null || !jwtTokenValidator.isValid(token)) {
            logger.warn("Rejected request to {} — missing or invalid JWT", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Id", jwtTokenValidator.extractUserId(token))
                .header("X-User-Email", jwtTokenValidator.extractEmail(token))
                .header("X-User-Role", jwtTokenValidator.extractRole(token))
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}