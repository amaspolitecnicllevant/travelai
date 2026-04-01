#!/usr/bin/env bash
# =============================================================
# TravelAI — Script d'inicialització completa del projecte
# v2 — Inclou GDPR/LOPD complet (obligatori + recomanat + bones pràctiques)
# Executar des de la carpeta arrel on vols el projecte
# =============================================================
set -e

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${GREEN}✓${NC} $1"; }
info() { echo -e "${CYAN}→${NC} $1"; }
warn() { echo -e "${YELLOW}!${NC} $1"; }

ROOT="$(pwd)"
BACK="$ROOT/travelai-backend"
FRONT="$ROOT/travelai-frontend"
JAVA="$BACK/src/main/java/com/travelai"
RES="$BACK/src/main/resources"
TEST="$BACK/src/test/java/com/travelai"

echo ""
echo "  ████████╗██████╗  █████╗ ██╗   ██╗███████╗██╗      █████╗ ██╗"
echo "  ╚══██╔══╝██╔══██╗██╔══██╗██║   ██║██╔════╝██║     ██╔══██╗██║"
echo "     ██║   ██████╔╝███████║██║   ██║█████╗  ██║     ███████║██║"
echo "     ██║   ██╔══██╗██╔══██║╚██╗ ██╔╝██╔══╝  ██║     ██╔══██║██║"
echo "     ██║   ██║  ██║██║  ██║ ╚████╔╝ ███████╗███████╗██║  ██║██║"
echo "     ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝╚══════╝╚═╝  ╚═╝╚═╝"
echo ""
info "Inicialitzant projecte en: $ROOT"
echo ""

# =============================================================
# 1. ESTRUCTURA DE DIRECTORIS
# =============================================================
info "Creant estructura de directoris..."

mkdir -p "$JAVA/config"
mkdir -p "$JAVA/domain/auth"
mkdir -p "$JAVA/domain/user"
mkdir -p "$JAVA/domain/trip"
mkdir -p "$JAVA/domain/ai"
mkdir -p "$JAVA/domain/rating"
mkdir -p "$JAVA/domain/notification"
mkdir -p "$JAVA/domain/legal"
mkdir -p "$JAVA/shared/dto"
mkdir -p "$JAVA/shared/exception"
mkdir -p "$JAVA/shared/mapper"
mkdir -p "$JAVA/shared/util"
mkdir -p "$JAVA/shared/audit"
mkdir -p "$RES/db/migration"
mkdir -p "$TEST/domain/trip"
mkdir -p "$TEST/domain/ai"
mkdir -p "$TEST/domain/legal"
mkdir -p "$FRONT/src/api"
mkdir -p "$FRONT/src/composables"
mkdir -p "$FRONT/src/stores"
mkdir -p "$FRONT/src/router"
mkdir -p "$FRONT/src/assets"
mkdir -p "$FRONT/src/components/common"
mkdir -p "$FRONT/src/components/trip"
mkdir -p "$FRONT/src/components/ai"
mkdir -p "$FRONT/src/components/map"
mkdir -p "$FRONT/src/components/legal"
mkdir -p "$FRONT/src/views/auth"
mkdir -p "$FRONT/src/views/trips"
mkdir -p "$FRONT/src/views/profile"
mkdir -p "$FRONT/src/views/legal"
mkdir -p "$ROOT/nginx"
mkdir -p "$ROOT/.vscode"
mkdir -p "$ROOT/docs/legal"

log "Directoris creats"

# =============================================================
# 2. ARXIUS ARREL
# =============================================================
info "Escrivint arxius arrel..."

cat > "$ROOT/.env" <<'EOF'
# TravelAI — Variables d'entorn locals
# MAI commitear aquest arxiu

# Base de dades
POSTGRES_PASSWORD=travelai_dev

# Redis
REDIS_PASSWORD=redis_dev

# JWT
JWT_SECRET=travelai-super-secret-jwt-key-local-dev-only-xxxxxxxxxxxxxxxx

# MinIO
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123

# Ollama IA
OLLAMA_MODEL=qwen2.5:7b
AI_PROVIDER=ollama

# App
APP_NAME=TravelAI
APP_VERSION=1.0.0
APP_CONTACT_EMAIL=privacidad@travelai.local
APP_DPO_EMAIL=dpo@travelai.local

# GDPR
GDPR_DELETION_DAYS=30
GDPR_LOG_RETENTION_DAYS=365
GDPR_MIN_AGE=14

# (Producció amb Claude API)
# ANTHROPIC_API_KEY=sk-ant-...
# AI_PROVIDER=claude
EOF
log ".env"

cat > "$ROOT/.env.example" <<'EOF'
POSTGRES_PASSWORD=canviar_aixo
REDIS_PASSWORD=canviar_aixo
JWT_SECRET=minim-256bits-alfanumeric-random-aqui
MINIO_ACCESS_KEY=canviar_aixo
MINIO_SECRET_KEY=canviar_aixo
OLLAMA_MODEL=qwen2.5:7b
AI_PROVIDER=ollama
APP_CONTACT_EMAIL=privacidad@domini.com
APP_DPO_EMAIL=dpo@domini.com
GDPR_DELETION_DAYS=30
GDPR_LOG_RETENTION_DAYS=365
GDPR_MIN_AGE=14
EOF
log ".env.example"

cat > "$ROOT/.gitignore" <<'EOF'
.env
.env.*
!.env.example
travelai-backend/target/
*.class
*.jar
*.war
travelai-frontend/node_modules/
travelai-frontend/dist/
travelai-frontend/.env*.local
.idea/
.vscode/settings.json
*.iml
postgres_data/
redis_data/
minio_data/
ollama_data/
docs/legal/signed/
.DS_Store
Thumbs.db
EOF
log ".gitignore"

# =============================================================
# 3. DOCKER COMPOSE
# =============================================================
cat > "$ROOT/docker-compose.yml" <<'EOF'
version: '3.9'

networks:
  travelai-net:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
  minio_data:
  ollama_data:
  maven_cache:

services:

  postgres:
    image: postgres:16-alpine
    container_name: travelai-postgres
    networks: [travelai-net]
    environment:
      POSTGRES_DB: travelai
      POSTGRES_USER: travelai
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-travelai_dev}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U travelai -d travelai"]
      interval: 5s
      timeout: 5s
      retries: 10
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: travelai-redis
    networks: [travelai-net]
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD:-redis_dev}
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-redis_dev}", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10
    restart: unless-stopped

  minio:
    image: minio/minio:latest
    container_name: travelai-minio
    networks: [travelai-net]
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY:-minioadmin}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY:-minioadmin123}
    volumes:
      - minio_data:/data
    ports:
      - "9000:9000"
      - "9001:9001"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  ollama:
    image: ollama/ollama:latest
    container_name: travelai-ollama
    networks: [travelai-net]
    volumes:
      - ollama_data:/root/.ollama
    ports:
      - "11434:11434"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
      interval: 10s
      timeout: 5s
      retries: 15
      start_period: 30s
    restart: unless-stopped

  ollama-init:
    image: curlimages/curl:latest
    container_name: travelai-ollama-init
    networks: [travelai-net]
    depends_on:
      ollama:
        condition: service_healthy
    entrypoint: >
      sh -c "
        echo 'Descarregant model ${OLLAMA_MODEL:-qwen2.5:7b}...' &&
        curl -s -X POST http://ollama:11434/api/pull
          -H 'Content-Type: application/json'
          -d '{\"name\": \"${OLLAMA_MODEL:-qwen2.5:7b}\", \"stream\": false}' &&
        echo 'Model llest.'
      "
    restart: "no"

  backend:
    build:
      context: ./travelai-backend
      dockerfile: Dockerfile
      target: development
    container_name: travelai-backend
    networks: [travelai-net]
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_URL: jdbc:postgresql://postgres:5432/travelai
      DB_USERNAME: travelai
      DB_PASSWORD: ${POSTGRES_PASSWORD:-travelai_dev}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:-redis_dev}
      OLLAMA_BASE_URL: http://ollama:11434
      OLLAMA_MODEL: ${OLLAMA_MODEL:-qwen2.5:7b}
      AI_PROVIDER: ${AI_PROVIDER:-ollama}
      JWT_SECRET: ${JWT_SECRET:-cambiar-esto-en-produccion-minimo-256bits}
      JWT_EXPIRATION_MS: 86400000
      JWT_REFRESH_EXPIRATION_MS: 604800000
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY:-minioadmin}
      MINIO_SECRET_KEY: ${MINIO_SECRET_KEY:-minioadmin123}
      MINIO_BUCKET: travelai
      APP_FRONTEND_URL: http://localhost:5173
      APP_CORS_ORIGINS: http://localhost:5173,http://localhost:80
      APP_CONTACT_EMAIL: ${APP_CONTACT_EMAIL:-privacidad@travelai.local}
      APP_DPO_EMAIL: ${APP_DPO_EMAIL:-dpo@travelai.local}
      GDPR_DELETION_DAYS: ${GDPR_DELETION_DAYS:-30}
      GDPR_LOG_RETENTION_DAYS: ${GDPR_LOG_RETENTION_DAYS:-365}
      GDPR_MIN_AGE: ${GDPR_MIN_AGE:-14}
    ports:
      - "8080:8080"
    volumes:
      - ./travelai-backend/src:/app/src
      - maven_cache:/root/.m2
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      ollama:
        condition: service_healthy
    restart: unless-stopped

  frontend:
    build:
      context: ./travelai-frontend
      dockerfile: Dockerfile.dev
    container_name: travelai-frontend
    networks: [travelai-net]
    environment:
      VITE_API_BASE_URL: http://localhost/api/v1
      VITE_WS_URL: ws://localhost/ws
      VITE_APP_NAME: TravelAI
      VITE_GDPR_MIN_AGE: 14
    ports:
      - "5173:5173"
    volumes:
      - ./travelai-frontend/src:/app/src
      - ./travelai-frontend/public:/app/public
      - /app/node_modules
    depends_on:
      - backend
    restart: unless-stopped

  nginx:
    image: nginx:1.25-alpine
    container_name: travelai-nginx
    networks: [travelai-net]
    volumes:
      - ./nginx/nginx.dev.conf:/etc/nginx/nginx.conf:ro
    ports:
      - "80:80"
    depends_on:
      - backend
      - frontend
    restart: unless-stopped
EOF
log "docker-compose.yml"

# =============================================================
# 4. NGINX
# =============================================================
cat > "$ROOT/nginx/nginx.dev.conf" <<'EOF'
worker_processes 1;
events { worker_connections 1024; }

http {
  include       /etc/nginx/mime.types;
  default_type  application/octet-stream;
  sendfile      on;
  keepalive_timeout 65;

  # Capçaleres de seguretat GDPR/LOPD
  add_header X-Content-Type-Options    "nosniff" always;
  add_header X-Frame-Options           "SAMEORIGIN" always;
  add_header X-XSS-Protection          "1; mode=block" always;
  add_header Referrer-Policy           "strict-origin-when-cross-origin" always;
  add_header Permissions-Policy        "geolocation=(), microphone=(), camera=()" always;
  add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

  upstream backend  { server backend:8080; }
  upstream frontend { server frontend:5173; }

  server {
    listen 80;
    server_name localhost;
    client_max_body_size 20M;

    location /api/ {
      proxy_pass         http://backend;
      proxy_http_version 1.1;
      proxy_set_header   Host              $host;
      proxy_set_header   X-Real-IP         $remote_addr;
      proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
      proxy_read_timeout    300s;
      proxy_connect_timeout 10s;
      proxy_send_timeout    300s;
    }

    location /api/v1/ai/ {
      proxy_pass         http://backend;
      proxy_http_version 1.1;
      proxy_set_header   Host              $host;
      proxy_set_header   X-Real-IP         $remote_addr;
      proxy_set_header   Connection        "";
      proxy_buffering    off;
      proxy_cache        off;
      chunked_transfer_encoding on;
      proxy_read_timeout    600s;
      proxy_send_timeout    600s;
    }

    location /ws {
      proxy_pass         http://backend;
      proxy_http_version 1.1;
      proxy_set_header   Upgrade    $http_upgrade;
      proxy_set_header   Connection "upgrade";
      proxy_set_header   Host       $host;
      proxy_read_timeout 3600s;
    }

    location /actuator {
      proxy_pass http://backend;
      allow 127.0.0.1;
      allow 172.16.0.0/12;
      deny  all;
    }

    location / {
      proxy_pass         http://frontend;
      proxy_http_version 1.1;
      proxy_set_header   Upgrade    $http_upgrade;
      proxy_set_header   Connection "upgrade";
      proxy_set_header   Host       $host;
      proxy_read_timeout 3600s;
    }
  }
}
EOF
log "nginx/nginx.dev.conf"

# =============================================================
# 5. BACKEND — Dockerfile i pom.xml
# =============================================================
info "Escrivint backend Spring Boot..."

cat > "$BACK/Dockerfile" <<'EOF'
FROM maven:3.9-eclipse-temurin-21 AS development
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
EXPOSE 8080
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.jvmArguments=-XX:+UseVirtualThreads"]

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS production
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java","-XX:+UseVirtualThreads","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]
EOF
log "Dockerfile backend"

cat > "$BACK/pom.xml" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
  </parent>
  <groupId>com.travelai</groupId>
  <artifactId>travelai-backend</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <properties>
    <java.version>21</java.version>
    <spring-ai.version>1.0.0</spring-ai.version>
    <mapstruct.version>1.6.2</mapstruct.version>
    <jjwt.version>0.12.6</jjwt.version>
    <minio.version>8.5.12</minio.version>
  </properties>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-bom</artifactId>
        <version>${spring-ai.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-webflux</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-websocket</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-oauth2-client</artifactId></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jjwt.version}</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>
    <dependency><groupId>org.springframework.session</groupId><artifactId>spring-session-data-redis</artifactId></dependency>
    <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-ollama-spring-boot-starter</artifactId></dependency>
    <dependency><groupId>io.minio</groupId><artifactId>minio</artifactId><version>${minio.version}</version></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    <dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId><version>${mapstruct.version}</version></dependency>
    <dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId><version>${mapstruct.version}</version><scope>provided</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-mail</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-devtools</artifactId><scope>runtime</scope><optional>true</optional></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.retry</groupId><artifactId>spring-retry</artifactId></dependency>
    <dependency><groupId>org.springframework</groupId><artifactId>spring-aspects</artifactId></dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></exclude>
          </excludes>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <source>21</source><target>21</target>
          <annotationProcessorPaths>
            <path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></path>
            <path><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId><version>${mapstruct.version}</version></path>
          </annotationProcessorPaths>
        </configuration>
      </plugin>
    </plugins>
  </build>
  <repositories>
    <repository>
      <id>spring-milestones</id>
      <n>Spring Milestones</n>
      <url>https://repo.spring.io/milestone</url>
      <snapshots><enabled>false</enabled></snapshots>
    </repository>
  </repositories>
</project>
EOF
log "pom.xml"

# =============================================================
# 6. APPLICATION.YML
# =============================================================
cat > "$RES/application.yml" <<'EOF'
spring:
  application:
    name: travelai-backend
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/travelai}
    username: ${DB_USERNAME:travelai}
    password: ${DB_PASSWORD:travelai_dev}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_schema: public
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:redis_dev}
      timeout: 2000ms
  session:
    store-type: redis
    timeout: 86400s
  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        model: ${OLLAMA_MODEL:qwen2.5:7b}
        options:
          temperature: 0.7
          num-ctx: 8192
          num-predict: 4096
          top-p: 0.9
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}

server:
  port: 8080
  compression:
    enabled: true
    mime-types: application/json,text/plain,text/event-stream
  error:
    include-message: always

jwt:
  secret: ${JWT_SECRET:dev-secret-change-in-prod}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}
  refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION_MS:604800000}

minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin123}
  bucket: ${MINIO_BUCKET:travelai}

app:
  frontend-url: ${APP_FRONTEND_URL:http://localhost:5173}
  cors-origins: ${APP_CORS_ORIGINS:http://localhost:5173,http://localhost:80}
  contact-email: ${APP_CONTACT_EMAIL:privacidad@travelai.local}
  dpo-email: ${APP_DPO_EMAIL:dpo@travelai.local}
  ai:
    provider: ${AI_PROVIDER:ollama}
    max-retries: 3
    retry-delay-ms: 1500
  gdpr:
    deletion-days: ${GDPR_DELETION_DAYS:30}
    log-retention-days: ${GDPR_LOG_RETENTION_DAYS:365}
    min-age: ${GDPR_MIN_AGE:14}
    privacy-policy-version: "1.0"
    terms-version: "1.0"
  security:
    max-login-attempts: 5
    lockout-minutes: 15
    password-min-length: 8

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.travelai: DEBUG
    org.springframework.security: WARN
    org.springframework.ai: DEBUG
  pattern:
    console: "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
EOF
log "application.yml"

# =============================================================
# 7. MIGRACIONS FLYWAY
# =============================================================
info "Escrivint migracions Flyway..."

cat > "$RES/db/migration/V1__init_schema.sql" <<'EOF'
-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── USERS ─────────────────────────────────────────────────────
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255),
    name         VARCHAR(100) NOT NULL,
    bio          TEXT,
    avatar_url   VARCHAR(500),
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    provider     VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    provider_id  VARCHAR(255),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    -- GDPR: camps de control de compte
    age_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- ── FOLLOWS ───────────────────────────────────────────────────
CREATE TABLE follows (
    follower_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id),
    CHECK (follower_id <> following_id)
);

-- ── TRIPS ─────────────────────────────────────────────────────
CREATE TABLE trips (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    destination     VARCHAR(200) NOT NULL,
    destination_lat DOUBLE PRECISION,
    destination_lng DOUBLE PRECISION,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    trip_type       VARCHAR(30)  NOT NULL,
    budget          VARCHAR(20)  NOT NULL,
    travelers       INTEGER      NOT NULL DEFAULT 1,
    visibility      VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',
    notes           TEXT,
    cover_image_url VARCHAR(500),
    ai_generated    BOOLEAN      NOT NULL DEFAULT FALSE,
    tags            TEXT[],
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CHECK (end_date >= start_date),
    CHECK (travelers > 0)
);
CREATE INDEX idx_trips_user_id     ON trips(user_id);
CREATE INDEX idx_trips_visibility  ON trips(visibility);
CREATE INDEX idx_trips_destination ON trips USING gin(destination gin_trgm_ops);
CREATE INDEX idx_trips_trip_type   ON trips(trip_type);
CREATE INDEX idx_trips_start_date  ON trips(start_date);

-- ── DAYS ──────────────────────────────────────────────────────
CREATE TABLE days (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id    UUID        NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    day_number INTEGER     NOT NULL,
    date       DATE        NOT NULL,
    title      VARCHAR(200),
    ai_notes   TEXT,
    user_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (trip_id, day_number)
);
CREATE INDEX idx_days_trip_id ON days(trip_id);

-- ── ACTIVITIES ────────────────────────────────────────────────
CREATE TABLE activities (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    day_id         UUID         NOT NULL REFERENCES days(id) ON DELETE CASCADE,
    time           VARCHAR(5),
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    location       VARCHAR(200),
    lat            DOUBLE PRECISION,
    lng            DOUBLE PRECISION,
    type           VARCHAR(30)  NOT NULL,
    estimated_cost NUMERIC(10,2),
    currency       VARCHAR(3)   DEFAULT 'EUR',
    tips           TEXT,
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_activities_day_id ON activities(day_id);

-- ── RATINGS ───────────────────────────────────────────────────
CREATE TABLE ratings (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id    UUID        NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score      SMALLINT    NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (trip_id, user_id)
);
CREATE INDEX idx_ratings_trip_id ON ratings(trip_id);

-- ── FAVORITES ─────────────────────────────────────────────────
CREATE TABLE favorites (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trip_id    UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, trip_id)
);

-- ── NOTIFICATIONS ─────────────────────────────────────────────
CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(50) NOT NULL,
    title      VARCHAR(200),
    body       TEXT,
    data       JSONB,
    read       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read);

-- ── REFRESH TOKENS ────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- ── AUDIT LOG (GDPR — registre d'accions sensibles) ───────────
CREATE TABLE audit_logs (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID        REFERENCES users(id) ON DELETE SET NULL,
    action     VARCHAR(100) NOT NULL,
    entity     VARCHAR(50),
    entity_id  UUID,
    ip_address INET,
    user_agent TEXT,
    details    JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_user_id   ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action    ON audit_logs(action);
CREATE INDEX idx_audit_logs_created   ON audit_logs(created_at);

-- ── CONSENT LOGS (GDPR — registre de consentiments) ──────────
CREATE TABLE consent_logs (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(50) NOT NULL,
    version      VARCHAR(20) NOT NULL,
    accepted     BOOLEAN     NOT NULL,
    ip_address   INET,
    user_agent   TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_consent_logs_user_id ON consent_logs(user_id);

-- ── DELETION REQUESTS (GDPR — dret a l'oblit) ────────────────
CREATE TABLE deletion_requests (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requested_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    scheduled_for TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '30 days',
    completed_at  TIMESTAMPTZ,
    reason        TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);
CREATE INDEX idx_deletion_requests_status       ON deletion_requests(status);
CREATE INDEX idx_deletion_requests_scheduled    ON deletion_requests(scheduled_for);

-- ── LOGIN ATTEMPTS (seguretat — anti brute-force) ─────────────
CREATE TABLE login_attempts (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email        VARCHAR(255) NOT NULL,
    ip_address   INET,
    success      BOOLEAN NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_login_attempts_email      ON login_attempts(email, created_at);
CREATE INDEX idx_login_attempts_ip         ON login_attempts(ip_address, created_at);

-- ── LEGAL DOCUMENTS (versions dels documents legals) ──────────
CREATE TABLE legal_documents (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type         VARCHAR(50)  NOT NULL,
    version      VARCHAR(20)  NOT NULL,
    content      TEXT         NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    published_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (type, version)
);
CREATE INDEX idx_legal_documents_type_active ON legal_documents(type, active);
EOF
log "V1__init_schema.sql"

cat > "$RES/db/migration/V2__seed_data.sql" <<'EOF'
-- Admin: Admin1234!
INSERT INTO users (username, email, password, name, role, age_verified)
VALUES ('admin','admin@travelai.local',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RnUqRCBOu',
        'Admin TravelAI','ADMIN', TRUE);

-- Demo: Demo1234!
INSERT INTO users (username, email, password, name, bio, age_verified)
VALUES ('demo','demo@travelai.local',
        '$2a$12$9RW/LD5QYSvpbS6lgWpHyOHvn8eRpBqDGkajUdBLOTVOiQjxFKKMm',
        'Usuari Demo','Viatger apassionat', TRUE);

-- Documents legals inicials (plantilles — cal substituir pel text real)
INSERT INTO legal_documents (type, version, content) VALUES
('PRIVACY_POLICY', '1.0', 'PLANTILLA: Cal redactar la Política de Privacitat real amb un advocat.'),
('TERMS',          '1.0', 'PLANTILLA: Cal redactar els Termes dÚs reals amb un advocat.'),
('COOKIES',        '1.0', 'PLANTILLA: Cal redactar la Política de Cookies real amb un advocat.');
EOF
log "V2__seed_data.sql"

cat > "$RES/db/migration/V3__rating_view.sql" <<'EOF'
CREATE VIEW trip_rating_summary AS
SELECT
    trip_id,
    ROUND(AVG(score)::numeric, 2) AS avg_score,
    COUNT(*)                       AS total_ratings,
    COUNT(CASE WHEN score = 5 THEN 1 END) AS five_stars,
    COUNT(CASE WHEN score = 4 THEN 1 END) AS four_stars,
    COUNT(CASE WHEN score = 3 THEN 1 END) AS three_stars,
    COUNT(CASE WHEN score = 2 THEN 1 END) AS two_stars,
    COUNT(CASE WHEN score = 1 THEN 1 END) AS one_star
FROM ratings
GROUP BY trip_id;
EOF
log "V3__rating_view.sql"

# =============================================================
# 8. JAVA — arxius base
# =============================================================
info "Escrivint Java base..."

cat > "$JAVA/TravelAiApplication.java" <<'EOF'
package com.travelai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableAsync
@EnableScheduling
public class TravelAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelAiApplication.class, args);
    }
}
EOF
log "TravelAiApplication.java"

cat > "$JAVA/domain/ai/OllamaService.java" <<'EOF'
package com.travelai.domain.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OllamaService {

    private final OllamaChatModel chatModel;

    public Flux<String> streamChat(String systemPrompt, String userMessage) {
        var prompt = buildPrompt(systemPrompt, userMessage);
        log.debug("Iniciant stream Ollama — {} chars", userMessage.length());
        return chatModel.stream(prompt)
            .map(res -> res.getResult().getOutput().getContent())
            .filter(chunk -> chunk != null && !chunk.isEmpty())
            .onErrorMap(e -> new AiException("Error en stream: " + e.getMessage()));
    }

    @Retryable(retryFor = AiException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 1500, multiplier = 2))
    public String chat(String systemPrompt, String userMessage) {
        try {
            return chatModel.call(buildPrompt(systemPrompt, userMessage))
                .getResult().getOutput().getContent();
        } catch (Exception e) {
            throw new AiException("Error en crida a Ollama: " + e.getMessage());
        }
    }

    private Prompt buildPrompt(String system, String user) {
        return new Prompt(
            List.of(new SystemMessage(system), new UserMessage(user)),
            OllamaOptions.builder().withFormat("json").withTemperature(0.7f).build()
        );
    }
}
EOF
log "OllamaService.java"

cat > "$JAVA/domain/ai/AiException.java" <<'EOF'
package com.travelai.domain.ai;

public class AiException extends RuntimeException {
    public AiException(String msg) { super(msg); }
    public AiException(String msg, Throwable cause) { super(msg, cause); }
}
EOF

cat > "$JAVA/shared/exception/ResourceNotFoundException.java" <<'EOF'
package com.travelai.shared.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String code;
    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }
}
EOF

cat > "$JAVA/shared/exception/GlobalExceptionHandler.java" <<'EOF'
package com.travelai.shared.exception;

import com.travelai.domain.ai.AiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    record ApiError(String code, String message, Object details, Instant timestamp) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
        return ResponseEntity.badRequest()
            .body(new ApiError("VALIDATION_ERROR", "Dades invàlides", errors, Instant.now()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(ex.getCode(), ex.getMessage(), null, Instant.now()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiError("INVALID_CREDENTIALS", "Email o contrasenya incorrectes", null, Instant.now()));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiError> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.LOCKED)
            .body(new ApiError("ACCOUNT_LOCKED", "Compte bloquejat temporalment per massa intents fallits", null, Instant.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiError("ACCESS_DENIED", "Sense permisos per a aquesta acció", null, Instant.now()));
    }

    @ExceptionHandler(AiException.class)
    public ResponseEntity<ApiError> handleAi(AiException ex) {
        log.error("AI error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ApiError("AI_ERROR", ex.getMessage(), null, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError("INTERNAL_ERROR", "Error intern del servidor", null, Instant.now()));
    }
}
EOF
log "Exceptions Java"

# =============================================================
# 9. FRONTEND
# =============================================================
info "Escrivint frontend Vue 3..."

cat > "$FRONT/Dockerfile.dev" <<'EOF'
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
EOF

cat > "$FRONT/package.json" <<'EOF'
{
  "name": "travelai-frontend",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.2.0",
    "@vueuse/core": "^11.0.0",
    "axios": "^1.7.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.1.0",
    "vite": "^5.4.0",
    "tailwindcss": "^3.4.0",
    "postcss": "^8.4.0",
    "autoprefixer": "^10.4.0",
    "@tailwindcss/forms": "^0.5.7",
    "@tailwindcss/typography": "^0.5.15"
  }
}
EOF

cat > "$FRONT/vite.config.js" <<'EOF'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws':  { target: 'ws://localhost:8080', ws: true }
    }
  },
  build: {
    target: 'esnext',
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia'],
          http:   ['axios']
        }
      }
    }
  }
})
EOF

cat > "$FRONT/tailwind.config.js" <<'EOF'
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50:'#eef2ff',100:'#e0e7ff',200:'#c7d2fe',300:'#a5b4fc',
          400:'#818cf8',500:'#6366f1',600:'#4f46e5',700:'#4338ca',
          800:'#3730a3',900:'#312e81'
        }
      },
      fontFamily: { sans: ['Inter','ui-sans-serif','system-ui','sans-serif'] }
    }
  },
  plugins: [require('@tailwindcss/forms'), require('@tailwindcss/typography')]
}
EOF

cat > "$FRONT/postcss.config.js" <<'EOF'
export default { plugins: { tailwindcss: {}, autoprefixer: {} } }
EOF

cat > "$FRONT/index.html" <<'EOF'
<!DOCTYPE html>
<html lang="ca">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>TravelAI — Planifica el teu viatge amb IA</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
EOF

cat > "$FRONT/src/main.js" <<'EOF'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
EOF

cat > "$FRONT/src/assets/main.css" <<'EOF'
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  body { @apply bg-gray-50 text-gray-900 antialiased; }
}

@layer components {
  .btn-primary {
    @apply inline-flex items-center justify-center px-4 py-2 rounded-lg
           bg-primary-600 text-white font-medium text-sm
           hover:bg-primary-700 focus:outline-none focus:ring-2
           focus:ring-primary-500 focus:ring-offset-2
           disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-150;
  }
  .btn-secondary {
    @apply inline-flex items-center justify-center px-4 py-2 rounded-lg
           bg-white text-gray-700 font-medium text-sm border border-gray-300
           hover:bg-gray-50 focus:outline-none focus:ring-2
           focus:ring-primary-500 focus:ring-offset-2 transition-colors duration-150;
  }
  .input {
    @apply block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm
           placeholder-gray-400 shadow-sm focus:border-primary-500
           focus:outline-none focus:ring-1 focus:ring-primary-500;
  }
  .card { @apply bg-white rounded-xl shadow-sm border border-gray-100 p-6; }
  .checkbox-label {
    @apply flex items-start gap-3 text-sm text-gray-600 cursor-pointer;
  }
  .checkbox-label input[type="checkbox"] {
    @apply mt-0.5 rounded border-gray-300 text-primary-600
           focus:ring-primary-500 cursor-pointer flex-shrink-0;
  }
}
EOF

cat > "$FRONT/src/App.vue" <<'EOF'
<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
onMounted(() => auth.fetchMe())
</script>
<template>
  <RouterView />
</template>
EOF
log "Frontend base"

# ── API ──────────────────────────────────────────────────────
cat > "$FRONT/src/api/index.js" <<'EOF'
import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let isRefreshing = false
let queue = []
const drain = (err, token) => { queue.forEach(p => err ? p.reject(err) : p.resolve(token)); queue = [] }

api.interceptors.response.use(res => res, async err => {
  const orig = err.config
  if (err.response?.status !== 401 || orig._retry) return Promise.reject(err)
  if (isRefreshing) return new Promise((res, rej) => queue.push({ resolve: res, reject: rej }))
    .then(t => { orig.headers.Authorization = `Bearer ${t}`; return api(orig) })
  orig._retry = true
  isRefreshing = true
  const rt = localStorage.getItem('refreshToken')
  if (!rt) { isRefreshing = false; localStorage.clear(); window.location = '/login'; return Promise.reject(err) }
  try {
    const { data } = await axios.post(`${api.defaults.baseURL}/auth/refresh`, { refreshToken: rt })
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    drain(null, data.accessToken)
    orig.headers.Authorization = `Bearer ${data.accessToken}`
    return api(orig)
  } catch (e) { drain(e, null); localStorage.clear(); window.location = '/login'; return Promise.reject(e) }
  finally { isRefreshing = false }
})

export default api
EOF

cat > "$FRONT/src/api/auth.js" <<'EOF'
import api from './index'

export const authApi = {
  login:          (data)  => api.post('/auth/login', data),
  register:       (data)  => api.post('/auth/register', data),
  logout:         ()      => api.post('/auth/logout'),
  refresh:        (rt)    => api.post('/auth/refresh', { refreshToken: rt }),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword:  (data)  => api.post('/auth/reset-password', data),
}
EOF

cat > "$FRONT/src/api/trips.js" <<'EOF'
import api from './index'

export const tripsApi = {
  getAll:    (params) => api.get('/trips', { params }),
  getFeed:   (params) => api.get('/trips/feed', { params }),
  getById:   (id)     => api.get(`/trips/${id}`),
  create:    (data)   => api.post('/trips', data),
  update:    (id, d)  => api.put(`/trips/${id}`, d),
  remove:    (id)     => api.delete(`/trips/${id}`),
  publish:   (id)     => api.post(`/trips/${id}/publish`),
  unpublish: (id)     => api.post(`/trips/${id}/unpublish`),
  search:    (params) => api.get('/trips/search', { params }),
  duplicate: (id)     => api.post(`/trips/${id}/duplicate`),
}
EOF

cat > "$FRONT/src/api/users.js" <<'EOF'
import api from './index'

export const usersApi = {
  getMe:         ()         => api.get('/users/me'),
  updateMe:      (data)     => api.put('/users/me', data),
  getByUsername: (username) => api.get(`/users/${username}`),
  follow:        (username) => api.post(`/users/${username}/follow`),
  unfollow:      (username) => api.delete(`/users/${username}/follow`),
  getTrips:      (username) => api.get(`/users/${username}/trips`),
  getStats:      (username) => api.get(`/users/${username}/stats`),
}
EOF

cat > "$FRONT/src/api/legal.js" <<'EOF'
import api from './index'

export const legalApi = {
  getPrivacyPolicy:  ()       => api.get('/legal/privacy-policy'),
  getTerms:          ()       => api.get('/legal/terms'),
  getCookiePolicy:   ()       => api.get('/legal/cookies'),
  saveConsent:       (data)   => api.post('/users/me/consent', data),
  exportMyData:      ()       => api.get('/users/me/data-export', { responseType: 'blob' }),
  requestDeletion:   (reason) => api.post('/users/me/delete-request', { reason }),
  cancelDeletion:    ()       => api.delete('/users/me/delete-request'),
}
EOF
log "API modules"

# ── STORES ───────────────────────────────────────────────────
cat > "$FRONT/src/stores/auth.js" <<'EOF'
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const user        = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const accessToken = ref(localStorage.getItem('accessToken') || null)
  const loading     = ref(false)
  const error       = ref(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin    = computed(() => user.value?.role === 'ADMIN')

  async function login(email, password) {
    loading.value = true; error.value = null
    try {
      const { data } = await api.post('/auth/login', { email, password })
      _save(data); return true
    } catch (e) { error.value = e.response?.data?.message || 'Credencials incorrectes'; return false }
    finally { loading.value = false }
  }

  async function register(payload) {
    loading.value = true; error.value = null
    try {
      const { data } = await api.post('/auth/register', payload)
      _save(data); return true
    } catch (e) { error.value = e.response?.data?.message || 'Error en registrar-se'; return false }
    finally { loading.value = false }
  }

  async function logout() {
    try { await api.post('/auth/logout') } catch { /* ok */ } finally { _clear() }
  }

  async function fetchMe() {
    if (!accessToken.value) return
    try {
      const { data } = await api.get('/users/me')
      user.value = data
      localStorage.setItem('user', JSON.stringify(data))
    } catch { _clear() }
  }

  function _save({ user: u, accessToken: at, refreshToken: rt }) {
    user.value = u; accessToken.value = at
    localStorage.setItem('user', JSON.stringify(u))
    localStorage.setItem('accessToken', at)
    localStorage.setItem('refreshToken', rt)
  }
  function _clear() {
    user.value = null; accessToken.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  return { user, accessToken, loading, error, isLoggedIn, isAdmin,
           login, register, logout, fetchMe }
})
EOF

cat > "$FRONT/src/stores/trips.js" <<'EOF'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { tripsApi } from '@/api/trips'

export const useTripsStore = defineStore('trips', () => {
  const trips   = ref([])
  const current = ref(null)
  const loading = ref(false)
  const error   = ref(null)

  async function fetchFeed(params = {}) {
    loading.value = true
    try { const { data } = await tripsApi.getFeed(params); trips.value = data.content }
    catch (e) { error.value = e.message }
    finally { loading.value = false }
  }

  async function fetchById(id) {
    loading.value = true
    try { const { data } = await tripsApi.getById(id); current.value = data; return data }
    catch (e) { error.value = e.message; return null }
    finally { loading.value = false }
  }

  async function create(payload) {
    const { data } = await tripsApi.create(payload)
    return data
  }

  async function update(id, payload) {
    const { data } = await tripsApi.update(id, payload)
    if (current.value?.id === id) current.value = data
    return data
  }

  return { trips, current, loading, error, fetchFeed, fetchById, create, update }
})
EOF

cat > "$FRONT/src/stores/consent.js" <<'EOF'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { legalApi } from '@/api/legal'

export const useConsentStore = defineStore('consent', () => {
  const cookiesAccepted = ref(localStorage.getItem('cookiesAccepted') === 'true')
  const showBanner      = ref(!cookiesAccepted.value)

  async function acceptAll() {
    cookiesAccepted.value = true
    showBanner.value      = false
    localStorage.setItem('cookiesAccepted', 'true')
    try {
      await legalApi.saveConsent({
        type: 'COOKIES', version: '1.0', accepted: true
      })
    } catch { /* continuar encara que falli */ }
  }

  function rejectAll() {
    cookiesAccepted.value = false
    showBanner.value      = false
    localStorage.setItem('cookiesAccepted', 'false')
  }

  return { cookiesAccepted, showBanner, acceptAll, rejectAll }
})
EOF
log "Stores"

# ── COMPOSABLES ──────────────────────────────────────────────
cat > "$FRONT/src/composables/useAiStream.js" <<'EOF'
import { ref, readonly } from 'vue'

export function useAiStream() {
  const streaming  = ref(false)
  const progress   = ref('')
  const rawBuffer  = ref('')
  const days       = ref([])
  const error      = ref(null)
  const controller = ref(null)

  const base  = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const token = () => localStorage.getItem('accessToken')

  const generate  = (id)              => _stream(`${base}/ai/trips/${id}/generate`, 'POST')
  const refineDay = (id, day, prompt) => _stream(`${base}/ai/trips/${id}/days/${day}/refine`, 'POST', { prompt })
  const refineAll = (id, prompt)      => _stream(`${base}/ai/trips/${id}/refine-all`, 'POST', { prompt })
  const cancel    = ()                => { controller.value?.abort(); streaming.value = false }

  async function _stream(url, method, body = null) {
    cancel()
    streaming.value = true; error.value = null
    rawBuffer.value = ''; days.value = []
    progress.value  = 'Connectant amb la IA...'
    controller.value = new AbortController()
    try {
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream',
                   Authorization: `Bearer ${token()}` },
        body: body ? JSON.stringify(body) : null,
        signal: controller.value.signal
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const reader = res.body.getReader()
      const dec    = new TextDecoder()
      let pending  = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        pending += dec.decode(value, { stream: true })
        const lines = pending.split('\n'); pending = lines.pop()
        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          const raw = line.slice(5).trim()
          if (raw) _handle(raw)
        }
      }
    } catch (e) {
      if (e.name !== 'AbortError') { error.value = e.message; progress.value = 'Error en la generació' }
    } finally { streaming.value = false }
    return days.value
  }

  function _handle(raw) {
    try {
      const e = JSON.parse(raw)
      if (e.type === 'start')              progress.value = e.message || 'Generant...'
      else if (e.type === 'chunk')       { rawBuffer.value += e.content || ''; progress.value = 'Escrivint...' }
      else if (e.type === 'day_complete' && e.day) { days.value = [...days.value, e.day]; progress.value = `Dia ${e.dayNumber} llest` }
      else if (e.type === 'complete')      progress.value = 'Itinerari completat'
      else if (e.type === 'error')         error.value = e.message
    } catch { /* chunk parcial */ }
  }

  return { streaming: readonly(streaming), progress: readonly(progress),
           rawBuffer: readonly(rawBuffer), days: readonly(days), error: readonly(error),
           generate, refineDay, refineAll, cancel }
}
EOF
log "Composables"

# ── ROUTER ───────────────────────────────────────────────────
cat > "$FRONT/src/router/index.js" <<'EOF'
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/',                   name: 'home',           component: () => import('@/views/HomeView.vue') },
  { path: '/explore',            name: 'explore',        component: () => import('@/views/ExploreView.vue') },
  { path: '/trips/:id',          name: 'trip-detail',    component: () => import('@/views/trips/TripDetailView.vue') },
  { path: '/profile/:username',  name: 'public-profile', component: () => import('@/views/profile/PublicProfileView.vue') },
  { path: '/login',              name: 'login',          component: () => import('@/views/auth/LoginView.vue'),    meta: { guestOnly: true } },
  { path: '/register',           name: 'register',       component: () => import('@/views/auth/RegisterView.vue'), meta: { guestOnly: true } },
  { path: '/feed',               name: 'feed',           component: () => import('@/views/FeedView.vue'),          meta: { requiresAuth: true } },
  { path: '/trips/new',          name: 'trip-create',    component: () => import('@/views/trips/CreateTripView.vue'),  meta: { requiresAuth: true } },
  { path: '/trips/:id/edit',     name: 'trip-edit',      component: () => import('@/views/trips/EditTripView.vue'),    meta: { requiresAuth: true } },
  { path: '/trips/:id/planner',  name: 'trip-planner',   component: () => import('@/views/trips/TripPlannerView.vue'), meta: { requiresAuth: true } },
  { path: '/profile',            name: 'my-profile',     component: () => import('@/views/profile/MyProfileView.vue'), meta: { requiresAuth: true } },
  // Pàgines legals — sempre públiques
  { path: '/privacy',            name: 'privacy',        component: () => import('@/views/legal/PrivacyPolicyView.vue') },
  { path: '/terms',              name: 'terms',          component: () => import('@/views/legal/TermsView.vue') },
  { path: '/cookies',            name: 'cookies',        component: () => import('@/views/legal/CookiePolicyView.vue') },
  { path: '/legal',              name: 'legal-notice',   component: () => import('@/views/legal/LegalNoticeView.vue') },
  { path: '/my-data',            name: 'my-data',        component: () => import('@/views/legal/MyDataView.vue'), meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*',    name: 'not-found',      component: () => import('@/views/NotFoundView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: (to, from, saved) => saved || { top: 0 }
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) next({ name: 'login', query: { redirect: to.fullPath } })
  else if (to.meta.guestOnly && auth.isLoggedIn) next({ name: 'feed' })
  else next()
})

export default router
EOF
log "Router"

# ── VIEWS PLACEHOLDER ────────────────────────────────────────
for view in HomeView ExploreView FeedView NotFoundView; do
  cat > "$FRONT/src/views/${view}.vue" <<EOF
<script setup>// TODO: implementar ${view}</script>
<template><div class="p-8 text-center"><h1 class="text-2xl font-semibold text-gray-800">${view}</h1></div></template>
EOF
done

for view in LoginView RegisterView; do
  cat > "$FRONT/src/views/auth/${view}.vue" <<EOF
<script setup>// TODO: implementar ${view}</script>
<template><div class="min-h-screen flex items-center justify-center bg-gray-50"><div class="card w-full max-w-md"><h1 class="text-xl font-semibold mb-4">${view}</h1></div></div></template>
EOF
done

for view in TripDetailView CreateTripView EditTripView TripPlannerView; do
  cat > "$FRONT/src/views/trips/${view}.vue" <<EOF
<script setup>// TODO: implementar ${view}</script>
<template><div class="p-8"><h1 class="text-2xl font-semibold text-gray-800">${view}</h1></div></template>
EOF
done

for view in MyProfileView PublicProfileView; do
  cat > "$FRONT/src/views/profile/${view}.vue" <<EOF
<script setup>// TODO: implementar ${view}</script>
<template><div class="p-8"><h1 class="text-2xl font-semibold text-gray-800">${view}</h1></div></template>
EOF
done

# Vistes legals — amb contingut mínim funcional
for view in PrivacyPolicyView TermsView CookiePolicyView LegalNoticeView; do
  cat > "$FRONT/src/views/legal/${view}.vue" <<EOF
<script setup>
import { ref, onMounted } from 'vue'
import { legalApi } from '@/api/legal'
const content = ref('Carregant...')
onMounted(async () => {
  try {
    const map = { PrivacyPolicyView: 'getPrivacyPolicy', TermsView: 'getTerms',
                  CookiePolicyView: 'getCookiePolicy' }
    // TODO: carregar contingut real de l'API
    content.value = 'Contingut pendent de redacció per un advocat especialitzat en protecció de dades.'
  } catch { content.value = 'Error carregant el contingut.' }
})
</script>
<template>
  <div class="max-w-3xl mx-auto px-4 py-12">
    <h1 class="text-2xl font-semibold text-gray-900 mb-6">${view}</h1>
    <div class="prose prose-gray max-w-none">
      <p class="text-amber-700 bg-amber-50 border border-amber-200 rounded-lg p-4 mb-6">
        Aquest document és una plantilla. Cal substituir-lo per text redactat per un advocat especialitzat en RGPD/LOPD.
      </p>
      <p>{{ content }}</p>
    </div>
  </div>
</template>
EOF
done

cat > "$FRONT/src/views/legal/MyDataView.vue" <<'EOF'
<script setup>
import { ref } from 'vue'
import { legalApi } from '@/api/legal'

const loading    = ref(false)
const requested  = ref(false)
const error      = ref(null)

async function exportData() {
  loading.value = true
  try {
    const res  = await legalApi.exportMyData()
    const url  = URL.createObjectURL(res.data)
    const a    = document.createElement('a')
    a.href     = url
    a.download = 'les-meves-dades-travelai.json'
    a.click()
    URL.revokeObjectURL(url)
  } catch { error.value = 'Error exportant les dades. Torna-ho a intentar.' }
  finally { loading.value = false }
}

async function requestDeletion() {
  if (!confirm('Estàs segur? El compte s\'esborrarà definitivament en 30 dies.')) return
  loading.value = true
  try { await legalApi.requestDeletion('Sol·licitud de l\'usuari'); requested.value = true }
  catch { error.value = 'Error en la sol·licitud. Contacta amb privacidad@travelai.local' }
  finally { loading.value = false }
}
</script>
<template>
  <div class="max-w-2xl mx-auto px-4 py-12">
    <h1 class="text-2xl font-semibold text-gray-900 mb-2">Les meves dades</h1>
    <p class="text-gray-500 mb-8">Gestiona les teves dades personals d'acord amb el RGPD.</p>

    <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 text-red-700 text-sm">{{ error }}</div>
    <div v-if="requested" class="bg-green-50 border border-green-200 rounded-lg p-4 mb-6 text-green-700 text-sm">
      Sol·licitud rebuda. El teu compte s'esborrarà en 30 dies. Rebràs un email de confirmació.
    </div>

    <div class="space-y-4">
      <div class="card">
        <h2 class="font-medium text-gray-900 mb-1">Descarregar les meves dades</h2>
        <p class="text-sm text-gray-500 mb-4">Exporta totes les teves dades en format JSON (dret d'accés i portabilitat — RGPD Art. 15 i 20).</p>
        <button @click="exportData" :disabled="loading" class="btn-primary">
          {{ loading ? 'Exportant...' : 'Descarregar dades' }}
        </button>
      </div>

      <div class="card border-red-100">
        <h2 class="font-medium text-gray-900 mb-1">Esborrar el meu compte</h2>
        <p class="text-sm text-gray-500 mb-4">El teu compte i totes les dades personals s'esborraran definitivament en 30 dies (dret a l'oblit — RGPD Art. 17). Els viatges públics quedaran anonimitzats.</p>
        <button @click="requestDeletion" :disabled="loading || requested"
                class="inline-flex items-center px-4 py-2 rounded-lg bg-red-600 text-white text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition-colors">
          Sol·licitar esborrat del compte
        </button>
      </div>

      <div class="card">
        <h2 class="font-medium text-gray-900 mb-1">Contacte per a drets RGPD</h2>
        <p class="text-sm text-gray-500">Per a qualsevol consulta sobre les teves dades (rectificació, limitació, oposició) contacta amb:</p>
        <p class="text-sm font-medium text-primary-600 mt-2">privacidad@travelai.local</p>
      </div>
    </div>
  </div>
</template>
EOF
log "Views (incloses legals i MyDataView)"

# ── VSCODE ───────────────────────────────────────────────────
cat > "$ROOT/travelai.code-workspace" <<'EOF'
{
  "folders": [
    { "name": "arrel",    "path": "." },
    { "name": "backend",  "path": "travelai-backend" },
    { "name": "frontend", "path": "travelai-frontend" }
  ],
  "settings": {
    "editor.formatOnSave": true,
    "editor.defaultFormatter": "esbenp.prettier-vscode",
    "[java]": { "editor.defaultFormatter": "redhat.java" },
    "java.configuration.updateBuildConfiguration": "automatic",
    "explorer.fileNesting.enabled": true,
    "explorer.fileNesting.patterns": {
      "*.vue": "${capture}.spec.js",
      "pom.xml": "*.xml",
      "package.json": "package-lock.json, .npmrc, postcss.config.js, tailwind.config.js"
    },
    "files.exclude": { "**/node_modules": true, "**/target": true }
  },
  "extensions": {
    "recommendations": [
      "Vue.volar", "redhat.java", "vscjava.vscode-spring-boot-dashboard",
      "vscjava.vscode-java-pack", "esbenp.prettier-vscode",
      "bradlc.vscode-tailwindcss", "ms-azuretools.vscode-docker",
      "humao.rest-client", "mikestead.dotenv",
      "streetsidesoftware.code-spell-checker-spanish"
    ]
  }
}
EOF

cat > "$ROOT/.vscode/tasks.json" <<'EOF'
{
  "version": "2.0.0",
  "tasks": [
    { "label": "🤖 Llançar tots els agents (tmux)", "type": "shell",
      "command": "bash parallel_agents.sh && tmux attach -t travelai-agents",
      "options": { "cwd": "${workspaceFolder}" },
      "presentation": { "reveal": "always", "panel": "new", "title": "Agents" },
      "problemMatcher": [] },
    { "label": "🔐 Agent AUTH", "type": "shell",
      "command": "cd travelai-backend/src/main/java/com/travelai/domain/auth && claude",
      "presentation": { "reveal": "always", "panel": "new", "title": "Agent AUTH" }, "problemMatcher": [] },
    { "label": "✈️  Agent TRIP", "type": "shell",
      "command": "cd travelai-backend/src/main/java/com/travelai/domain/trip && claude",
      "presentation": { "reveal": "always", "panel": "new", "title": "Agent TRIP" }, "problemMatcher": [] },
    { "label": "🧠 Agent AI", "type": "shell",
      "command": "cd travelai-backend/src/main/java/com/travelai/domain/ai && claude",
      "presentation": { "reveal": "always", "panel": "new", "title": "Agent AI" }, "problemMatcher": [] },
    { "label": "⚖️  Agent LEGAL/GDPR", "type": "shell",
      "command": "cd travelai-backend/src/main/java/com/travelai/domain/legal && claude",
      "presentation": { "reveal": "always", "panel": "new", "title": "Agent LEGAL" }, "problemMatcher": [] },
    { "label": "🖥️  Agent FRONTEND", "type": "shell",
      "command": "cd travelai-frontend/src && claude",
      "presentation": { "reveal": "always", "panel": "new", "title": "Agent FRONTEND" }, "problemMatcher": [] },
    { "label": "🗄️  Agent INFRA", "type": "shell",
      "command": "cd travelai-backend/src/main/resources && claude",
      "presentation": { "reveal": "always", "panel": "new", "title": "Agent INFRA" }, "problemMatcher": [] },
    { "label": "📋 Logs Docker", "type": "shell",
      "command": "docker-compose logs -f backend frontend",
      "presentation": { "reveal": "always", "panel": "new", "title": "Logs" }, "problemMatcher": [], "isBackground": true },
    { "label": "🚀 Docker: arrancar tot", "type": "shell",
      "command": "docker-compose up",
      "presentation": { "reveal": "always", "panel": "new", "title": "Docker" }, "problemMatcher": [], "isBackground": true },
    { "label": "⚙️  Docker: només infra", "type": "shell",
      "command": "docker-compose up -d postgres redis minio ollama",
      "presentation": { "reveal": "always", "panel": "shared" }, "problemMatcher": [] },
    { "label": "🧪 Tests backend", "type": "shell",
      "command": "cd travelai-backend && mvn test",
      "presentation": { "reveal": "always", "panel": "new", "title": "Tests" },
      "problemMatcher": ["$javac"], "group": { "kind": "test", "isDefault": true } }
  ]
}
EOF
log "VSCode workspace i tasks.json"

cat > "$ROOT/requests.http" <<'EOF'
@base = http://localhost/api/v1
@token = ENGANXAR_TOKEN_AQUI

### Health
GET http://localhost:8080/actuator/health

### Registre (amb consentiment GDPR)
POST {{base}}/auth/register
Content-Type: application/json

{
  "name": "Test User",
  "username": "testuser",
  "email": "test@test.com",
  "password": "Test1234!",
  "ageConfirmed": true,
  "privacyPolicyAccepted": true,
  "privacyPolicyVersion": "1.0",
  "termsAccepted": true,
  "termsVersion": "1.0"
}

### Login
POST {{base}}/auth/login
Content-Type: application/json

{ "email": "demo@travelai.local", "password": "Demo1234!" }

### Perfil propi
GET {{base}}/users/me
Authorization: Bearer {{token}}

### Exportar dades (GDPR Art. 15 i 20)
GET {{base}}/users/me/data-export
Authorization: Bearer {{token}}

### Sol·licitar esborrat de compte (GDPR Art. 17)
POST {{base}}/users/me/delete-request
Authorization: Bearer {{token}}
Content-Type: application/json

{ "reason": "Ja no vull usar el servei" }

### Crear viatge (visibility PRIVATE per defecte — privacy by default)
POST {{base}}/trips
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "title": "Tòquio a la tardor",
  "destination": "Tokyo, Japan",
  "destinationLat": 35.6762,
  "destinationLng": 139.6503,
  "startDate": "2025-11-01",
  "endDate": "2025-11-07",
  "tripType": "CULTURAL",
  "budget": "MEDIUM",
  "travelers": 2,
  "visibility": "PRIVATE",
  "notes": "Volem veure el koyo"
}

### Generar itinerari (SSE)
POST {{base}}/ai/trips/TRIP_ID_AQUI/generate
Authorization: Bearer {{token}}
Accept: text/event-stream

### Política de privacitat
GET {{base}}/legal/privacy-policy
EOF
log "requests.http"

# =============================================================
# RESUM FINAL
# =============================================================
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  TravelAI v2 inicialitzat (amb GDPR/LOPD) ${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "  ${YELLOW}Nous respecte al GDPR/LOPD:${NC}"
echo -e "  - Taules: audit_logs, consent_logs, deletion_requests, login_attempts, legal_documents"
echo -e "  - API: /users/me/data-export, /users/me/delete-request, /legal/**"
echo -e "  - Vistes: /privacy, /terms, /cookies, /legal, /my-data"
echo -e "  - Capçaleres de seguretat a Nginx"
echo -e "  - Privacy by default: viatges en PRIVATE per defecte"
echo -e "  - Consentiment explícit al registre"
echo ""
echo -e "  ${YELLOW}Pròxims passos:${NC}"
echo ""
echo -e "  1. Obrir VSCode:"
echo -e "     ${CYAN}code travelai.code-workspace${NC}"
echo ""
echo -e "  2. Instal·lar deps frontend:"
echo -e "     ${CYAN}cd travelai-frontend && npm install && cd ..${NC}"
echo ""
echo -e "  3. Arrancar infraestructura:"
echo -e "     ${CYAN}docker-compose up -d postgres redis minio ollama${NC}"
echo ""
echo -e "  4. Descarregar model IA (una sola vegada, ~4.5 GB):"
echo -e "     ${CYAN}docker exec travelai-ollama ollama pull qwen2.5:7b${NC}"
echo ""
echo -e "  5. Arrancar tot:"
echo -e "     ${CYAN}docker-compose up -d backend frontend nginx${NC}"
echo ""
echo -e "  6. Llançar agents:"
echo -e "     ${CYAN}./parallel_agents.sh && tmux attach -t travelai-agents${NC}"
echo ""
echo -e "  ${YELLOW}IMPORTANT:${NC} Els documents legals (/privacy, /terms, /cookies)"
echo -e "  contenen plantilles. Cal substituir-les per text redactat"
echo -e "  per un advocat especialitzat en RGPD/LOPD abans del llançament."
echo ""
