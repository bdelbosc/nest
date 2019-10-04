# Training nuxeo-stream importer

## Build the extractor docker image

Build a jar and docker image:
```bash
mvn -nsu install -T4 -DskipTests=true -Pdocker
```

## Prepare the Nuxeo Stack

Add your `instance.clid` file to the nuxeo data directory
```bash
cp ~/my/nuxeo/instance.clid  ../stack-nuxeo-importer/data/nuxeo/data/
``` 

For linux user, check your user id
```bash
id -u
``` 
if your id is different than `1000` update the `../stack-nuxeo-importer/.env` file.

For Mac OS or Ubuntu 16.04 user, update your `/etc/hosts` add:
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

## Run the Extractor

```bash
docker run --rm --name extract -e "JAVA_TOOL_OPTIONS=-Dlog.type=kafka -Dkafka.bootstrap.servers=kafka:9092" --network container:kafka local/training-importer:1.0-SNAPSHOT /bjcp-2015.json 
```


### Extractor Options
Options to put in `JAVA_TOOL_OPTIONS` env, using `-D`.

| Option | default | Description |
| --- | ---: | --- |
|`log.type` | `chronicle` | Choose between `chronicle` or `kafka` | 
|`log.name` | `myLog` | Log name | 
|`log.size` | `4` | Number of partition in the Log | 
|`kafka.bootstrap.servers` | `localhost:9092`| The Kafka bootstrap servers | 
|`cq.path` | `/tmp/training`| The Chronicle Queue root path |

## Load the documents

```bash
./stack-nuxeo-importer/bin/training-import.sh
```

## Kafka

### Using Kafka sh scripts

List topics
```bash
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh  --zookeeper zookeeper:2181 --list
-- __consumer_offsets
-- nuxeo-audit
-- nuxeo-bjcp
-- nuxeo-bulk-automation
-- ...
```

Describe a topic
```bash
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh  --zookeeper zookeeper:2181 --describe --topic nuxeo-bjcp 
-- Topic:nuxeo-bjcp	PartitionCount:4	ReplicationFactor:1	Configs:
--	Topic: nuxeo-bjcp	Partition: 0	Leader: 1001	Replicas: 1001	Isr: 1001
--	Topic: nuxeo-bjcp	Partition: 1	Leader: 1001	Replicas: 1001	Isr: 1001
--	Topic: nuxeo-bjcp	Partition: 2	Leader: 1001	Replicas: 1001	Isr: 1001
--	Topic: nuxeo-bjcp	Partition: 3	Leader: 1001	Replicas: 1001	Isr: 1001
```

list consumer group
```bash
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
-- nuxeo-bulk-setSystemProperties
-- nuxeo-AuditLogWriter
-- nuxeo-bulk-automation
-- ...
```

Describe a consumer group
```bash
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group nuxeo-AuditLogWriter
-- GROUP                TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                            HOST            CLIENT-ID
-- nuxeo-AuditLogWriter nuxeo-audit     0          1039            1039            0               AuditLogWriter-13-666cc3d2-5a5d-409e-9347-6e1565fab4cc /192.168.16.7   AuditLogWriter-13
```

### Using Nuxeo stream.sh

list all lags
```bash
./bin/stream.sh lag -k
```

list lag for a Log
```bash
./bin/stream.sh lag -k -l audit
```

View all partitions lag
```bash
./bin/stream.sh lag -k -l bjcp --verbose
-- ## Log: bjcp partitions: 4
-- ### Group: StreamImporter.runDocumentConsumers
-- | partition | lag | pos | end | posOffset |?endOffset?|
-- | --- | ---: | ---: | ---: | ---: | ---: |
-- |All|0|143|143|32|39|
-- |0|0|38|38|38|38|
-- |1|0|39|39|39|39|
-- |2|0|32|32|32|32|
-- |3|0|34|34|34|34|
```

View latency of a stream, the record needs to be a computation Record 
```bash
./bin/stream.sh latency -k -l audit --verbose --codec avro
-- ## Log: audit partitions: 1
-- ### Group: AuditLogWriter
-- | partition | lag | latencyMs | latency | posTimestamp | posDate | curDate | pos | end | posOffset |?endOffset?| posKey |
-- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
-- |All|0|0|NA|1569851945716|2019-09-30T13:59:05.716Z|2019-09-30T14:22:49.502Z|1039|1039|1039|1039|0|
```

### Using KafkaHQ

http://kafkahq.docker.localhost/my-cluster/

## Stop your stack

```bash
# from stack-nuxeo-importer directory:
docker-compose down --volume
```
