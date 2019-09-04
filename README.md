# Rabbit Consumer [![Build Status](https://travis-ci.org/PaddyPowerBetfair/RabbitConsumer.svg?branch=master)](https://travis-ci.org/PaddyPowerBetfair/RabbitConsumer) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/ba7973a539c94e36bbd39e8f88f3573d)](https://www.codacy.com/app/rodoherty1/RabbitConsumer?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=PaddyPowerBetfair/RabbitConsumer&amp;utm_campaign=Badge_Grade) [![Coverage Status](https://coveralls.io/repos/github/PaddyPowerBetfair/RabbitConsumer/badge.svg?branch=master)](https://coveralls.io/github/PaddyPowerBetfair/RabbitConsumer?branch=master)

## What does it do?
This is a simple utility for reading json payloads that are published to a RabbitMQ Exchange and then writing them to a text file as valid json.

## How do I use it?
This utility is driven by command line arguments. Type `sbt "run --help"` to see them.
```
[info] Running com.ppb.rabbitconsumer.Main --help
      --host  <arg>
      --password  <arg>
      --port  <arg>
      --pretty
      --timeout  <arg>
      --use-ssl
      --username  <arg>
  -h, --help              Show help message

 trailing arguments:
  exchange (required)
  routingKey (required)
  file-name (required)
```

For example, to consume from exchange **exchange** with routing key **routingKey** and to save results to the **out.json**, simply type `sbt "run exchange routingKey out.json"`. To stop the process, press CTRL+C (SIGINT signal). 


### Integration Test Support

Place the Integration Test files files to src/it/scala folder. 
The test resource file can be provided in src/it/resource folder.
Place any test resources to src/it/resources folder.

In this example to pass the integration test Rabbitmq server should be running on default ports. 
Alternatively, docker instance can be started using following docker image:
```
docker run -d --hostname my-rabbit --name my-rabbit -p 15671-15672:15671-15672/tcp -p 5671-5672:5671-5672/tcp -p 61613:61613 rabbitmq:3-management
```
### Usage
```
$ sbt
$ console
$ it:test
```
### Adding Docker support to Travis

If docker support is needed in Travis Continous Integration update the .travis.yml file
```
services:
  - docker
 ```
 To start Docker Container in Travis Continous Integration 
 ```
 before_install:
  - docker run -d --hostname my-rabbit --name my-rabbit -p 15671-15672:15671-15672/tcp -p 5671-5672:5671-5672/tcp -p 61613:61613 rabbitmq:3-management
  - docker ps -a
 ```
  

## How can I contribute?
Please see [CONTRIBUTING.md](https://github.com/PaddyPowerBetfair/RabbitConsumer/blob/master/CONTRIBUTING.md).

## What licence is this released under?
This is released under a modified version of the BSD licence.
Please see [LICENCE.md](https://github.com/PaddyPowerBetfair/RabbitConsumer/blob/master/LICENCE.md).
