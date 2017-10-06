# Rabbit Consumer [![Build Status](https://travis-ci.org/PaddyPowerBetfair/RabbitConsumer.svg?branch=master)](https://travis-ci.org/PaddyPowerBetfair/RabbitConsumer) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/ba7973a539c94e36bbd39e8f88f3573d)](https://www.codacy.com/app/rodoherty1/RabbitConsumer?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=PaddyPowerBetfair/RabbitConsumer&amp;utm_campaign=Badge_Grade) [![Coverage Status](https://coveralls.io/repos/github/PaddyPowerBetfair/RabbitConsumer/badge.svg?branch=master)](https://coveralls.io/github/PaddyPowerBetfair/RabbitConsumer?branch=master)

## What does it do?
This is a simple utility for reading json payloads that are published to a RabbitMQ Exchange and then writing them to a text file as valid json.

## How do I use it?
This utility is driven by config files located in ```src/main/resources``` and is run from within an ```sbt console```.

For example, if ```src/main/resources/sample.json``` contains the following json
```
amqp {
  connections = [
    {
      ip           = "127.0.0.1"
      port         = 5672
      user         = "guest"
      password     = "guest"
      useSSL       = false
      exchangeName = "myExchange"
      queue        = "myQueue"
      routingKey   = ""
      fileName     = "~/output.json"
    }
  ]
}
```

then ```R.read("sample")``` will read all json messages from the specified Queue and write them all to file.

By default, ```local.conf``` is provided.

You may add your own conf files.  E.g. If you create ```myConf.conf``` then you may use it by calling ```R.read("myConf")```.

When you call```R.read("myConf")``` (for example), a queue is bound to the specified Exchange.

When you are done, make sure you call ```R.done("myConf")``` to delete the queues which were bound to the Exchange.

### Usage
```
$ sbt
$ console
$ R.local           // will read messages published to the Soccer UI Exchange and Soccer DM Exchange and write them to ~/scratch/dm-output and ~/scratch/ui-output.json
$ R.read("myconf")
$ R.done("myconf")
$ R.done("local")

## How can I contribute?
Please see [CONTRIBUTING.md](https://github.com/PaddyPowerBetfair/RabbitConsumer/blob/master/CONTRIBUTING.md).

## What licence is this released under?
This is released under a modified version of the BSD licence.
Please see [LICENCE.md](https://github.com/PaddyPowerBetfair/RabbitConsumer/blob/master/LICENCE.md).
