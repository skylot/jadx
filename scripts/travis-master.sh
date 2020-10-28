#!/usr/bin/env bash
set -e

# upload bundles to bintray unstable package
./gradlew clean dist
BINTRAY_PACKAGE=unstable bash scripts/bintray-upload.sh
