# Stage 1: Build client
FROM node:22-alpine AS client-builder

WORKDIR /app/client
COPY client/package*.json ./
RUN npm install
COPY client/ ./

RUN npm run build

# Stage 2: Build API
FROM node:22-alpine AS api-builder

WORKDIR /app
COPY api/package*.json ./
RUN npm install
COPY api/ ./
RUN npx tsp compile .

# Stage 3: Build server
FROM eclipse-temurin:25-jdk-alpine AS server-builder

WORKDIR /app
COPY server/gradlew ./
COPY server/gradle gradle
COPY server/settings.gradle.kts ./
COPY server/app app
COPY server/db db
RUN mkdir -p /api/tsp-output/schema
COPY --from=api-builder /app/tsp-output/schema/openapi.yaml /api/tsp-output/schema/openapi.yaml

RUN ./gradlew :app:distTar --no-daemon

# Stage 4: Runtime
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
COPY --from=client-builder /app/client/out /app/client
COPY --from=server-builder /app/app/build/distributions/app-bundle.tar /app/app-bundle.tar
COPY --from=server-builder /app/db/migrations /app/db/migrations
RUN tar -xf /app/app-bundle.tar

EXPOSE 8080

CMD ./app-bundle/bin/app
