package com.ppb.rabbitconsumer.examples

import java.io.File

import com.ppb.rabbitconsumer.{Configurations, ConnectionService, Cxn, RabbitConsumer}
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scalaz.concurrent.Task
import scalaz.stream._

object RabbitConsumerApp extends App {


  def getConfigsFromFile(configFile: File): Configurations =
    Configurations(configFile.getName, ConfigFactory.parseFile(configFile).getConfigList("amqp.connections").asScala.toList)

  val cxxn: (Config) => Cxn = (config) =>
        ConnectionService.newInit(config)

  val processMessages: Cxn => Process[Task, Unit] = cxn =>
    RabbitConsumer.getMessages(cxn.nextMessage).toSource pipe text.utf8Encode to io.chunkW(System.out)


  val consume:(File) => Unit =  getConfigsFromFile _ andThen RabbitConsumer.consumeMessages(cxxn, processMessages)

  val exitUsage:(String) => Unit = (message ) => {
    println(s"${message}  \n\t valid command line arguments are <connection config file>\n")
    System.exit(1);
  }


  /////////  execution starts //////////////

  println( "\n\n\t Rabbit Consumer App")
  println( "\n\n\t Copyright (c)  \n")
  println( s"\t Version := ${System.getProperty("prog.version")} , revision := ${System.getProperty("prog.revision")}\n ")


  if (args.length == 0) exitUsage("")
  else {
    consume(new File(args(0)))
  }
  println(" All done , exiting")

}
