#!/bin/bash

# Quick script to stop Docker services

echo "Stopping PostgreSQL and Redis..."

if command -v docker compose &> /dev/null; then
    docker compose down
else
    docker-compose down
fi

echo "Services stopped!"

