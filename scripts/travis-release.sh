#!/usr/bin/env bash
set -xe

npm install -g semantic-release
npm install -g semantic-release/exec
semantic-release
