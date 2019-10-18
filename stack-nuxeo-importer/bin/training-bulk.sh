#!/usr/bin/env bash

SERVER_URL=${SERVER_URL:-http://nuxeo.docker.localhost}
set -e
SCRIPT_PATH="$(cd "$(dirname "$0")"; pwd -P)"
curl -s -X POST "$SERVER_URL/nuxeo/api/v1/search/pp/default_search/bulk/myAction" -u Administrator:Administrator -H 'Content-Type: application/json' -d '{"fieldName":"bjcp:tags"}' | tee /tmp/bulk-command.txt | jq .
${SCRIPT_PATH}/bulk-status.sh
