#!/bin/bash

PROJECT_DATABASE='sample'
PROJECT_DIR="$(git rev-parse --show-toplevel)"

SCHEMA_FILE="$PROJECT_DIR/src/main/resources/db/sql/schema.sql"
CHANGELOG_FILE="$PROJECT_DIR/src/main/resources/changelog/changelog.sql"
DATA_DIR="$PROJECT_DIR/src/main/resources/data"
GENERATED_SQL_FILE="schema/all.sql"
GENERATED_TARGET_FILE="$PROJECT_DIR/local/target/$GENERATED_SQL_FILE"
POSTGRES_COMPOSE_PATH="$PROJECT_DIR/local/compose.yml"

function startPostgresql() {
  make postgresql-start
}

function initFile() {
  mkdir -p "$PROJECT_DIR/local/target/schema"
  echo "-- $(date "+%b_%d_%Y_%H.%M.%S")" >$GENERATED_TARGET_FILE
}

function createDatabase() {
  export PGPASSWORD='admin'
  docker-compose -f $POSTGRES_COMPOSE_PATH exec sample-db dropdb -U postgres $PROJECT_DATABASE --if-exists
  docker-compose -f $POSTGRES_COMPOSE_PATH exec sample-db createdb -U postgres $PROJECT_DATABASE
}

function appendFile() {
  printf '\tImporting: %s\n' "$(basename $1)"
  printf "\n\n" >>"$GENERATED_TARGET_FILE"
  cat $1 >>$GENERATED_TARGET_FILE
}

function appendDataFiles() {
  for file in $(find $DATA_DIR -name \*_data.sql); do
    appendFile $file
  done
}

function importFile() {
  docker-compose -f $POSTGRES_COMPOSE_PATH exec sample-db psql -v ON_ERROR_STOP=1 -U postgres -d $PROJECT_DATABASE -a -f /var/data/$GENERATED_SQL_FILE
  if [ $? -eq 0 ]; then
    echo "Success psql!"
  else
    echo "Failed psql!"
    exit 1
  fi
}

echo "Started database creation"
startPostgresql
initFile
appendFile $SCHEMA_FILE
appendFile $CHANGELOG_FILE
appendDataFiles
createDatabase
importFile
echo "Finished database creation"
