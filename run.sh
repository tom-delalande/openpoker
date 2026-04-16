#!/bin/bash

POSTGRES_PASSWORD=postgres \
  REDIS_USER=redis \
  REDIS_PASSWORD=redis \
  docker compose up --build
