#!/bin/sh

./gradlew :launchers:controlplane:shadowJar
./gradlew :launchers:dataplane:shadowJar
./gradlew :launchers:identityhub:shadowJar
./gradlew :launchers:issuerservice:shadowJar

./gradlew :launchers:controlplane:dockerize
./gradlew :launchers:dataplane:dockerize
./gradlew :launchers:identityhub:dockerize
./gradlew :launchers:issuerservice:dockerize
