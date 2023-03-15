#!/bin/bash

PROJECT_DIR="$(git rev-parse --show-toplevel)"
POSTGRES_COMPOSE_PATH="$PROJECT_DIR/local/compose.yml"

if [ -z `docker ps -q --no-trunc | grep $(docker-compose -f $POSTGRES_COMPOSE_PATH ps -q sample-db)` ]; then
  echo "Postgres is not running."
else
  docker-compose -f $POSTGRES_COMPOSE_PATH stop sample-db
fi
