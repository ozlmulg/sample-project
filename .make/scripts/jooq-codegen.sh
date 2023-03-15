#!/usr/bin/env bash

./mvnw clean install -U -DskipTests

./mvnw clean install -f pom.xml -Pjooq-codegen -DskipTests
