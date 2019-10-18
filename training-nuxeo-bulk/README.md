# Bulk Action and the Bulk Service

## Build the bundle

Build a jar:
```bash
mvn -nsu install -T4 -DskipTests=true
```

## Start the previous importer stack

```bash
cd ../stack-nuxeo-importer
docker-compose up
``` 

## Deploy the bundle into the stack-nuxeo-swm

```bash
docker cp ../training-nuxeo-bulk/target/training-nuxeo-bulk-1.0-SNAPSHOT.jar nuxeo:/opt/nuxeo/server/nxserver/bundles/
``` 

## Restart nuxeo

Using ctop restart nuxeo

## Run the bulk action

```bash
./bin/training-bulk.sh
``` 

