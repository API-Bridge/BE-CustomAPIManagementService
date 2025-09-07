# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Spring Boot 3.5.4 microservice** built with **Java 17** (Amazon Corretto) that operates as an API bridge service in a microservice architecture. The service is designed to be stateless, containerized, and relies on external shared infrastructure.

**Key Architecture Principles:**
- **Stateless JWT Authentication** via Auth0
- **Event-driven communication** using Apache Kafka
- **External shared infrastructure** (MySQL, Redis, Kafka are managed separately)
- **Container-first deployment** with Docker and Kubernetes support

## Essential Commands

### Development Workflow
```bash
# Start local development dependencies
docker-compose -f docker-compose.local.yml up -d

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Run specific test categories
./gradlew test --tests="*ControllerTest"
./gradlew test --tests="*IntegrationTest"
./gradlew test --tests="*UnitTest"

# Build for production
./gradlew build

# Create Docker image
docker build -t custom-api-svc:latest .
```

### Useful Development Commands
```bash
# Clean and rebuild
./gradlew clean build

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Check dependency vulnerabilities
./gradlew dependencyCheckAnalyze
```

## Core Architecture

### Microservice Design Pattern
This service follows **Domain-Driven Design** principles with clear layer separation:

- **Controllers** (`/controller/`) - REST API endpoints with OpenAPI documentation
- **Services** (`/service/`) - Business logic layer
- **Repositories** (`/repository/`) - Data access layer
- **Domain Entities** (`/domain/`) - JPA entities with shared BaseEntity
- **DTOs** (`/dto/`) - Data transfer objects with common BaseResponse
- **Events** (`/event/`) - Event-driven architecture using Kafka

### External Dependencies
The service depends on these **external shared services**:
- **MySQL 8.0** - Primary database (configured via `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- **Redis 7** - Caching and session store (configured via `REDIS_HOST`, `REDIS_PORT`)
- **Apache Kafka** - Event messaging (configured via `KAFKA_BOOTSTRAP_SERVERS`)
- **Auth0** - JWT authentication service (configured via `AUTH0_ISSUER_URI`, `AUTH0_AUDIENCE`)

### Security Architecture
- **JWT-based authentication** with Auth0 integration
- **Stateless design** - no server-side sessions
- **Method-level security** using `@PreAuthorize` and `@PostAuthorize`
- **Custom JWT validation** including audience validation

## Development Guidelines

### Testing Strategy
The codebase provides three distinct testing approaches:

1. **Unit Tests** (`BaseUnitTest`) - Pure unit tests with Mockito, no Spring context
2. **Integration Tests** (`BaseIntegrationTest`) - Full application with TestContainers
3. **Controller Tests** (`BaseControllerTest`) - Web layer tests with MockMvc

Always extend the appropriate base test class for your test type.

### Configuration Management
- **Environment-specific configs** in `application-{profile}.yml`
- **Environment variables** for external service connections
- **Spring profiles**: `dev`, `prod`, `test`
- **Feature toggles** via configuration properties

### Event-Driven Architecture
- Use `EventPublisher` for publishing domain events to Kafka
- Extend `BaseEvent` for all event types
- Events should be published asynchronously for better performance
- Follow the Domain Event pattern for business operations

### JPA and Database
- All entities must extend `BaseEntity` for audit fields
- Use **Soft Delete** pattern (deleted flag) instead of hard deletes
- **JPA Auditing** automatically handles createdAt/updatedAt
- Database schema is managed in `schema.sql`

## Important Patterns and Conventions

### API Design
- All endpoints return `BaseResponse<T>` wrapper for consistency
- Use OpenAPI 3.0 annotations (`@Operation`, `@Tag`) for documentation
- Follow RESTful conventions with proper HTTP status codes
- API base path is `/api/v1`

### Error Handling
- Implement global exception handlers in `/exception/` directory
- Use appropriate HTTP status codes
- Return structured error responses using BaseResponse

### Monitoring and Observability
- **Health checks** available at `/api/v1/health`
- **Prometheus metrics** at `/actuator/prometheus`
- **Distributed tracing** configured in application.yml
- **Structured logging** with correlation IDs

### Security Best Practices
- Never log or expose JWT tokens
- Use `@PreAuthorize` for method-level security
- Validate JWT audience claims using `AudienceValidator`
- Extract user info using `JwtUtils`

## Local Development Setup

1. **Start external dependencies:**
   ```bash
   docker-compose -f docker-compose.local.yml up -d
   ```

2. **Verify dependencies are running:**
   ```bash
   docker-compose -f docker-compose.local.yml ps
   ```

3. **Run the application:**
   ```bash
   ./gradlew bootRun
   ```

4. **Access development tools:**
   - Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
   - Health Check: http://localhost:8080/api/v1/health
   - Actuator: http://localhost:8080/actuator

## Production Deployment

### Environment Variables (Required)
```bash
# Database
DB_URL=jdbc:mysql://mysql-host:3306/customapi
DB_USERNAME=your-username
DB_PASSWORD=your-password

# Cache
REDIS_HOST=redis-host
REDIS_PORT=6379

# Messaging
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092

# Authentication
AUTH0_ISSUER_URI=https://your-domain.auth0.com/
AUTH0_AUDIENCE=https://api.your-service.com

# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
BUILD_VERSION=1.0.0
```

### Container Health Checks
The Dockerfile includes health check configuration that validates the `/api/v1/health` endpoint.

### Circuit Breaker Configuration
Resilience4j is configured for external service calls with:
- Circuit breaker for service failures
- Retry policies with exponential backoff
- Bulkhead pattern for concurrency control

## Working with This Codebase

### Adding New Features
1. Create domain entities extending `BaseEntity`
2. Implement repository interfaces
3. Create service classes with business logic
4. Add REST controllers with OpenAPI documentation
5. Write tests using appropriate base test classes
6. Add event publishing if needed

### Adding New Endpoints
1. Create DTOs in appropriate `/dto/request` or `/dto/response` folders
2. Add controller methods with proper security annotations
3. Use `BaseResponse<T>` for consistent API responses
4. Add OpenAPI documentation
5. Write controller tests extending `BaseControllerTest`

### Working with Events
1. Create event classes extending `BaseEvent`
2. Use `EventPublisher` to publish events
3. Create event listeners in `/event/listener/` directory
4. Test event publishing in integration tests

This microservice is designed to be deployed in containerized environments like Kubernetes where external dependencies are managed as shared services across multiple microservice instances.