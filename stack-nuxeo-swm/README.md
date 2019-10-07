# Training Nuxeo with Stream WorkManager and monitoring
## Make sure previous stack is down
```bash
# from the previous stack directory:
docker-compose down --volume
```

## Prepare the Nuxeo Stack

Add your `instance.clid` file to the nuxeo data directory
```bash
cp ~/my/nuxeo/instance.clid  ./data/nuxeo/data/
``` 

For linux user, check your user id
```bash
id -u
``` 
if your id is different than `1000` update the `.env` file.

If not already done for Mac OS or Ubuntu 16.04 user, update your `/etc/hosts` add:
```bash
127.0.0.1 nuxeo.docker.localhost
127.0.0.1 nuxeo-node.docker.localhost
127.0.0.1 elastic.docker.localhost
127.0.0.1 kibana.docker.localhost
127.0.0.1 grafana.docker.localhost
127.0.0.1 graphite.docker.localhost
127.0.0.1 kafkahq.docker.localhost
127.0.0.1 traefik.docker.localhost
```

## Start the stack

```bash
cd ../stack-nuxeo-importer/
docker-compose up
```

## Check the stack:

Traefik: http://traefik.docker.localhost

Nuxeo behind traefik: http://nuxeo.docker.localhost

Nuxeo direct access: http://nuxeo-node.docker.localhost

KafkaHQ: http://kafkahq.docker.localhost

Elasticsearch: http://elastic.docker.localhost

Graphite: http://graphite.docker.localhost

Grafana: http://grafana.docker.localhost

## stream.sh monitoring

http://grafana.docker.localhost
User: admin
Password: admin

## Run an import

```bash
./bin/import.sh
```

## Run a bulk command

Re-indexing using bulk service

```bash
./bin/bulk-reindex.sh 
{
  "commandId": "4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7"
}
Status of last command: 4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7
{
  "entity-type": "bulkStatus",
  "commandId": "4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7",
  "state": "SCROLLING_RUNNING",
  "processed": 0,
  "error": false,
  "errorCount": 0,
  "total": 0,
  "action": "index",
  "username": "Administrator",
  "submitted": "2019-10-04T14:54:31.090Z",
  "scrollStart": "2019-10-04T14:54:31.121Z",
  "scrollEnd": null,
  "processingStart": null,
  "processingEnd": null,
  "completed": null,
  "processingMillis": 0
}
```

Get the status again

```
./bin/bulk-status.sh 
Status of last command: 4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7
{
  "entity-type": "bulkStatus",
  "commandId": "4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7",
  "state": "RUNNING",
  "processed": 29,
  "error": false,
  "errorCount": 0,
  "total": 1029,
  "action": "index",
  "username": "Administrator",
  "submitted": "2019-10-04T14:54:31.090Z",
  "scrollStart": "2019-10-04T14:54:31.121Z",
  "scrollEnd": "2019-10-04T14:54:31.235Z",
  "processingStart": null,
  "processingEnd": null,
  "completed": null,
  "processingMillis": 0
}

./bin/bulk-status.sh 
Status of last command: 4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7
{
  "entity-type": "bulkStatus",
  "commandId": "4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7",
  "state": "COMPLETED",
  "processed": 1029,
  "error": false,
  "errorCount": 0,
  "total": 1029,
  "action": "index",
  "username": "Administrator",
  "submitted": "2019-10-04T14:54:31.090Z",
  "scrollStart": "2019-10-04T14:54:31.121Z",
  "scrollEnd": "2019-10-04T14:54:31.235Z",
  "processingStart": null,
  "processingEnd": null,
  "completed": "2019-10-04T14:54:37.407Z",
  "processingMillis": 0
}

```

View the command from the Stream:
```
./bin/bulk-scheduled.sh 
+ docker exec nuxeo /opt/nuxeo/server/bin/stream.sh tail -k -l bulk-command --codec avro -schema-store /var/lib/nuxeo/data/avro/ --data-size 5000
```
| offset | watermark | flag | key | length | data |
| --- | --- | --- | --- | ---: | --- |
|bulk-command-00:+1|2019-10-04 14:54:31.093:0|[DEFAULT]|4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7|131|{"id": "4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7", "action": "index", "query": "SELECT ecm:uuid FROM Document", "username": "Administrator", "repository": "default", "bucketSize": 1000, "batchSize": 25, "params": "{\"updateAlias\":true}"}|


View the result from the Stream:
```
$ ./bin/bulk-done.sh 
+ docker exec nuxeo /opt/nuxeo/server/bin/stream.sh tail -k -l bulk-done --codec avro -schema-store /var/lib/nuxeo/data/avro/ --data-size 5000
```
| offset | watermark | flag | key | length | data |
| --- | --- | --- | --- | ---: | --- |
|bulk-done-00:+1|2019-10-04 14:54:37.408:0|[DEFAULT]|4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7|112|{"commandId": "4262d0c2-fce4-4e9e-8e98-6c7c797a2cc7", "action": "index", "username": "Administrator", "delta": false, "errorCount": 0, "errorMessage": null, "processed": 1029, "state": "COMPLETED", "submitTime": 1570200871090, "scrollStartTime": 1570200871121, "scrollEndTime": 1570200871235, "processingStartTime": null, "processingEndTime": null, "completedTime": 1570200877407, "total": 1029, "processingDurationMillis": null, "result": null}|

## Generate a failure

Dump a record from a Stream and 

## Stop your stack

```bash
docker-compose down --volume
```
