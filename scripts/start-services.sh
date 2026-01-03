#!/bin/bash

# Quick script to start Docker services

echo "Starting PostgreSQL and Redis..."

if command -v docker compose &> /dev/null; then
    docker compose up -d
else
    docker-compose up -d
fi

echo "Services started!"
echo "PostgreSQL: localhost:5432"
echo "Redis: localhost:6379"

