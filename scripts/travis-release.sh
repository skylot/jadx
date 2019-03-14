#!/usr/bin/env bash
set -e

npm install -g semantic-release
npm install -g semantic-release/exec
semantic-release
