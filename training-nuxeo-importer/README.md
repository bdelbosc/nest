# Training nuxeo-stream importer

## Build the extractor image

Build a jar and docker image:
```bash
mvn -nsu install -T4 -DskipTests=true -Pdocker
```

## Running

### Start a Nuxeo stack

Add your `instance.clid` file to the nuxeo data directory
```bash
cp ~/my/nuxeo/instance.clid  ../stack-nuxeo-importer/data/nuxeo/data/
``` 

Edit the `docker-compose.yml` file to set the full path of the esdata volume

```bash
vi ../stack-nuxeo-importer/docker-compose.yml
```

Start the stack
```bash
cd ../stack-nuxeo-importer/
docker-compose up
```

### Run Extractor

```bash
docker run --rm --name extract -e "JAVA_TOOL_OPTIONS=-Dlog.type=kafka -Dkafka.bootstrap.servers=kafka:9092" --network container:kafka local/training-importer:1.0-SNAPSHOT /bjcp-2015.json /default-domain/workspaces 
```

### Load documents

```bash
./stack-nuxeo-importer/bin/training-import.sh
```

### Options
Options to put in `JAVA_TOOL_OPTIONS` env, using `-D`.

| Option | default | Description |
| --- | ---: | --- |
|`log.type` | `chronicle` | Choose between `chronicle` or `kafka` | 
|`log.name` | `myLog` | Log name | 
|`log.size` | `4` | Number of partition in the Log | 
|`timeout.seconds`| `600` | Application shutdown after timeout |
|`kafka.bootstrap.servers` | `localhost:9092`| The Kafka bootstrap servers | 
|`cq.path` | `/tmp/training`| The Chronicle Queue root path |

