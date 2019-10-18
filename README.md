# Training Nuxeo Stream 2019-10

## Requirements

- Java 11
- Maven >= 3.5
- docker >= 19.03
- docker-compose >= 1.21
- Some disk space
```bash
   docker system prune
   docker volume prune
```

### Nice to have

- jq
- ctop

### For Mac OS

Configure the limit of your Docker Engine so it can use 4 GiB of memory (and optionally 4 CPUs)

For Mac OS (or Ubuntu 16.04 user), update your `/etc/hosts` add:
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

## Download all docker images and base images

You can download in advance all necessary docker images to prevent traffic jam during the training:

```bash
docker-compose -f docker-compose-pull.yml pull
```

## Training sections

1. [Nuxeo Stream Lib and Kafka](./training-nuxeo-stream/README.md)
1. [Producer/Consumer pattern and Nuxeo Stream importer](./training-nuxeo-importer/README.md)
1. [Stream processor pattern in Nuxeo: audit, StreamWorkManager](./stack-nuxeo-swm/README.md)
1. [Create a Bulk Action and use the Bulk Service](./training-nuxeo-bulk/README.md)
