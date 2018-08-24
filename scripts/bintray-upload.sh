#!/usr/bin/env bash
set -xe

export JFROG_CLI_OFFER_CONFIG=false
export JFROG_CLI_LOG_LEVEL=DEBUG

npm install -g jfrog-cli-go

TARGET=skylot/jadx/${BINTRAY_PACKAGE}/v${JADX_VERSION}
CREDENTIALS="--user=skylot --key=${BINTRAY_KEY}"

jfrog bt version-create ${TARGET} ${CREDENTIALS} --desc=${JADX_VERSION}
jfrog bt upload 'build/jadx.*\.(zip|exe)' ${TARGET} ${CREDENTIALS} --regexp=true --publish=true
jfrog bt version-publish ${TARGET} ${CREDENTIALS}
