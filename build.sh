#!/bin/sh
./gradlew clean build

mkdir -p ./build/libs/
cp launcher-fancy/build/libs/*-all.jar ./build/libs/
cp launcher-builder/build/libs/*-all.jar ./build/libs/
cp launcher-bootstrap/build/libs/*-all.jar ./build/libs/
cp launcher/build/libs/*-all.jar ./build/libs/
cp creator-tools/build/libs/*-all.jar ./build/libs/

read -p "Press any key to continue..."