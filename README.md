# Training Nuxeo Stream 2019-10

## Requirements

- Java 11
- maven >= 3.5
- docker >= 19.03
- docker-compose >= 1.21

## Download all docker images and base images

You can donwload in advance all necessary docker images to prevent traffic jam during the training:

```bash
docker-compose -f docker-compose-pull.yml pull
```

## Training sections

1. [Nuxeo Stream Lib and Kafka](./training-nuxeo-stream/README.md)
1. [Nuxeo Stream Lib Producer/Consumer pattern](./training-nuxeo-importer/README.md)
1. [Nuxeo Steam, integration](./stack-nuxeo-swm)
