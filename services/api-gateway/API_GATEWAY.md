# API Gateway

**Port:** 8080 | **No database** | **Owner:** Nursultan | **Tech:** Spring Cloud Gateway

## Responsibility
Single entry point for all clients. JWT validation, request routing, rate limiting, CORS, request/response logging.

## Dependencies (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
```

## Route Configuration

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
      routes:
        # Auth (public routes go directly to user-service)
        - id: auth
          uri: http://user-service:8081
          predicates:
            - Path=/api/v1/auth/**
        
        # Users management
        - id: users
          uri: http://user-service:8081
          predicates:
            - Path=/api/v1/users/**
        
        # Employees, Departments, Positions
        - id: employees
          uri: http://employee-service:8082
          predicates:
            - Path=/api/v1/employees/**,/api/v1/departments/**,/api/v1/positions/**
        
        # Attendance
        - id: attendance
          uri: http://attendance-service:8083
          predicates:
            - Path=/api/v1/attendance/**
        
        # Leave
        - id: leave
          uri: http://leave-service:8084
          predicates:
            - Path=/api/v1/leave/**
        
        # Payroll
        - id: payroll
          uri: http://payroll-service:8085
          predicates:
            - Path=/api/v1/payroll/**
        
        # AI/ML
        - id: ai
          uri: http://ai-ml-service:8086
          predicates:
            - Path=/api/v1/ai/**
        
        # Reports
        - id: reports
          uri: http://reporting-service:8087
          predicates:
            - Path=/api/v1/reports/**
        
        # Notifications
        - id: notifications
          uri: http://notification-service:8088
          predicates:
            - Path=/api/v1/notifications/**
        
        # Integration & Settings
        - id: integration
          uri: http://integration-hub:8089
          predicates:
            - Path=/api/v1/integration/**,/api/v1/settings/**
        
        # Dashboard — served by reporting-service (cross-service read aggregation)
        - id: dashboard
          uri: http://reporting-service:8087
          predicates:
            - Path=/api/v1/dashboard/**

      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "https://hrms.nursnerv.uk"
              - "http://localhost:5173"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

## JWT Filter
```java
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
    
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login", "/api/v1/auth/refresh",
        "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
        "/api/actuator/health"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        
        String token = extractToken(exchange.getRequest());
        if (token == null || !jwtService.isValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        // Forward user info as headers to downstream services
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-User-Id", jwtService.extractUserId(token))
            .header("X-User-Email", jwtService.extractEmail(token))
            .header("X-User-Role", jwtService.extractRole(token))
            .build();
        
        return chain.filter(exchange.mutate().request(mutated).build());
    }
    
    @Override
    public int getOrder() { return -1; }
}
```

## Rate Limiting
```yaml
# Per-user rate limiting via Redis
- id: rate-limit
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 50     # 50 requests/second steady
        redis-rate-limiter.burstCapacity: 100    # burst up to 100
        key-resolver: "#{@userKeyResolver}"
```
