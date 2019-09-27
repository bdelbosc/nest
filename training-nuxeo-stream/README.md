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
cat | docker run -i --name producer -e "JAVA_TOOL_OPTIONS=-Dlog.type=kafka -Dkafka.bootstrap.servers=kafka:9092" --network container:kafka local/training-stream:1.0-SNAPSHOT 
```

### Consumer

Start a single consumer:
```bash
cd ../stack-training-nuxeo-stream
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
|`work.duration.ms`| `1000` | Work duration in ms |
|`timeout.seconds`| `600` | Application shutdown after timeout |
|`kafka.bootstrap.servers` | `localhost:9092`| The Kafka bootstrap servers | 
|`cq.path` | `/tmp/training`| The Chronicle Queue root path |

