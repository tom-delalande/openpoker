# OpenPoker

A real-time multiplayer poker platform built with Next.js and Kotlin.

## Architecture

- **Client**: Next.js React application with WebSocket support
- **Server**: Kotlin Ktor backend with PostgreSQL and Redis
- **API**: TypeScript API definitions

## Prerequisites

- Docker and Docker Compose
- Node.js 18+ (for local development)
- Kotlin (for server development)

## Quick Start

```bash
# Start all services with Docker
docker-compose up -d

# Or use the run script
./run.sh
```

Services:
- Client: http://localhost:3000
- Server: http://localhost:3001
- PostgreSQL: localhost:5432
- Redis: localhost:6379

## Development

### Client
```bash
cd client
npm install
npm run dev
```

### Server
```bash
cd server
./gradlew run
```

## Tech Stack

- **Frontend**: Next.js, React, TypeScript, Zustand
- **Backend**: Kotlin, Ktor, Exposed, PostgreSQL
- **Real-time**: WebSocket, Redis Pub/Sub
- **Infrastructure**: Docker, Docker Compose