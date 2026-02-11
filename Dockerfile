# Multi-stage Docker build for Lasso application

# Stage 1: Frontend Builder
FROM node:18-alpine AS frontend-builder

WORKDIR /app

# Install Java (required for shadow-cljs)
RUN apk add --no-cache openjdk11

# Copy package files and install dependencies
COPY package.json package-lock.json* ./
RUN npm ci

# Copy shadow-cljs and tailwind config
COPY shadow-cljs.edn tailwind.config.js ./
COPY resources/public/css/input.css ./resources/public/css/

# Copy frontend source code
COPY src/cljs ./src/cljs

# Build production frontend
RUN npx shadow-cljs release app
RUN npm run build:css

# Stage 2: Backend Builder
FROM clojure:temurin-11-tools-deps AS backend-builder

WORKDIR /app

# Copy deps.edn and download dependencies (cached layer)
COPY deps.edn ./
RUN clojure -P -X:uberjar

# Copy backend source code
COPY src/clj ./src/clj
COPY resources ./resources

# Build uberjar
RUN clojure -X:uberjar

# Stage 3: Runtime
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S lasso && \
    adduser -u 1001 -S lasso -G lasso

# Copy JAR from backend builder
COPY --from=backend-builder /app/target/lasso.jar ./lasso.jar

# Copy static frontend assets from frontend builder
COPY --from=frontend-builder /app/resources/public/js ./resources/public/js
COPY --from=frontend-builder /app/resources/public/css ./resources/public/css

# Copy index.html
COPY resources/public/index.html ./resources/public/

# Set ownership
RUN chown -R lasso:lasso /app

USER lasso

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Run the application
CMD ["java", "-jar", "lasso.jar"]
