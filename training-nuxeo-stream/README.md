# Training nuxeo-stream lib

## Build

Build a jar and docker image:
```bash
mvn -nsu install -T4 -DskipTests=true -Pdocker
```

## Running
### Kafka stack

```bash
cd ../stack-kafka/
docker-compose up
```

### Producer

```bash
cat | docker run --rm -i --name producer -e "JAVA_TOOL_OPTIONS=-Dlog.type=kafka -Dkafka.bootstrap.servers=kafka:9092" --network container:kafka local/training-stream:1.0-SNAPSHOT 
```

### Consumer

Start a single consumer:
```bash
cd ../stack-nuxeo-stream
docker-compose up
```

To scale up and down:
```bash
docker-compose up --scale consumer=5
```

### Options
Options to put in `JAVA_TOOL_OPTIONS` env, using `-D`.

| Option | default | Description |
| --- | ---: | --- |
|`app.type` | `producer` | Run a `producer` or a `consumer` | 
|`log.type` | `chronicle` | Choose between `chronicle` or `kafka` | 
|`log.name` | `myLog` | Log name | 
|`log.size` | `4` | Number of partition in the Log | 
|`log.codec` | `avro` | Codec choice to encode the Record | 
|`consumer.concurrency` | `2` | Number of consumer threads | 
|`consumer.group` | `myGroup` | Name of the consumer group | 
|`work.duration.ms`| `1000` | Work duration in ms |
|`timeout.seconds`| `600` | Application shutdown after timeout |
|`kafka.bootstrap.servers` | `localhost:9092`| The Kafka bootstrap servers | 
|`cq.path` | `/tmp/training`| The Chronicle Queue root path |

### Switch to log to debug
```bash
# switch the producer to debug 
docker exec -it $(docker ps -qf "name=^producer") perl -pi -w -e 's,info,debug,g' /my-log4j2.xml
```

## Using Kafka shell scripts

```bash
# list topics
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh  --zookeeper zookeeper:2181 --list
-- __consumer_offsets
-- training-myLog

# describe a topic
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh  --zookeeper zookeeper:2181 --describe --topic training-myLog 
-- Topic:training-myLog	PartitionCount:4	ReplicationFactor:1	Configs:
-- 	Topic: training-myLog	Partition: 0	Leader: 1001	Replicas: 1001	Isr: 1001
-- 	Topic: training-myLog	Partition: 1	Leader: 1001	Replicas: 1001	Isr: 1001
-- 	Topic: training-myLog	Partition: 2	Leader: 1001	Replicas: 1001	Isr: 1001
-- 	Topic: training-myLog	Partition: 3	Leader: 1001	Replicas: 1001	Isr: 1001

# list consumer group
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
-- training-myGroup

# describe a group
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group training-myGroup
-- GROUP            TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                    HOST            CLIENT-ID
-- training-myGroup training-myLog  0          1               1               0               myGroup-1-780854a2-0b10-49e8-88e0-1e4a1698b1e5 /192.168.64.6   myGroup-1
-- training-myGroup training-myLog  1          2               2               0               myGroup-1-780854a2-0b10-49e8-88e0-1e4a1698b1e5 /192.168.64.6   myGroup-1
-- training-myGroup training-myLog  2          1               1               0               myGroup-2-17b759b2-e50f-40c8-b939-dcd6e5af4ce5 /192.168.64.6   myGroup-2
-- training-myGroup training-myLog  3          2               2               0               myGroup-2-17b759b2-e50f-40c8-b939-dcd6e5af4ce5 /192.168.64.6   myGroup-2

```


### Using KafkaHQ

http://kafkahq.docker.localhost/

