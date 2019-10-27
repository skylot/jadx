#!/usr/bin/env bash
set -e

# upload coverage to codecov
./gradlew clean build jacocoTestReport
bash <(curl -s https://codecov.io/bash) || echo "Codecov did not collect coverage reports"

# run sonar checks
./gradlew clean sonarqube -Dsonar.host.url=${SONAR_HOST} -Dsonar.projectKey=jadx -Dsonar.organization=${SONAR_ORG} -Dsonar.login=${SONAR_TOKEN} || echo "Skip sonar build and upload"

# upload bundles to bintray unstable package
./gradlew clean dist
BINTRAY_PACKAGE=unstable bash scripts/bintray-upload.sh
