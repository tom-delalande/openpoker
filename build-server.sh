#!/bin/bash
set -e

cd "$(dirname "$0")/server"
./gradlew assemble
