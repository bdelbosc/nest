# Training nuxeo-stream Bulk Action

## Build the bundle

Build a jar:
```bash
mvn -nsu install -T4 -DskipTests=true
```

## Start the stack 

```bash
cd ../stack-nuxeo-swm
docker-compose up
``` 


## Deploy the bundle into the stack-nuxeo-swm

```bash
docker cp ../training-nuxeo-bulk/target/training-nuxeo-bulk-1.0-SNAPSHOT.jar nuxeo:/opt/nuxeo/server/nxserver/bundles/
``` 

## Restart nuxeo

```bash
./bin/nuxeoctl.sh restart
``` 


## Run the bulk action

```bash

docker-compose up
``` 

