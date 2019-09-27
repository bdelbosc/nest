#!/usr/bin/env bash

# List the 10 latest scheduled Bulk Command
set -x
docker exec nuxeo /opt/nuxeo/server/bin/stream.sh tail -k -l bulk-command --codec avro -schema-store /var/lib/nuxeo/data/avro/ --data-size 5000

