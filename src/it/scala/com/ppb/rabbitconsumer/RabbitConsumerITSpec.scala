package com.ppb.rabbitconsumer

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.UUID

import com.ppb.rabbitconsumer.RabbitConsumerAlgebra.Configurations
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._

import scala.collection.JavaConverters._
import argonaut._
import Argonaut._

import scala.io.Source
//import scalaz.stream.{Sink, _}


class RabbitConsumerITSpec extends FlatSpec  {

  val configFile:String = "src/it/resources/local.conf"
  val configurations:Configurations = Configurations(configFile, ConfigFactory.parseFile(new File(configFile)).getConfigList("amqp.connections").asScala.toList)

  val setUp:Config => Unit = (config) => {
    println("Inside setup "+ config)
    val rabbitConn:RabbitConnection = ConnectionService.rabbitConnection(config)
    val queueName = ConfigService.readQueue(config)
    rabbitConn.channel.queuePurge(queueName)
    println(" Publishing dummy to queue "+ queueName)
    for (i <- 1 to 3) {
      val map:Map[String, String] = Map("QueueName"->queueName,  "message" -> UUID.randomUUID().toString, "ID"-> i.toString)
      rabbitConn.channel.basicPublish("", queueName, null, map.asJson.toString().getBytes())
    }

    RabbitConsumer.readFromFile(configFile)

    println("closing connection")
    rabbitConn.connection.close(1000)
    Option(ConfigService.getFilename(config)) map(
      outputfile => { Files.deleteIfExists(Paths.get(outputfile)) }
    )getOrElse( println (" To Stdout") )

  }

  val verifyOrFail:Config => Unit = (config) => {
    Option(ConfigService.getFilename(config)) map(
      outputfile => {
        assert(Files.exists(Paths.get(outputfile)))
        assert(Files.size(Paths.get(outputfile)) > 0)
        println("============== "+ outputfile +"============") ;
        scala.io.Source.fromFile(outputfile).getLines foreach println _
      }
      )getOrElse(  )
  }



  configurations.configs.map(setUp)

  it should "read stream message to local file it" in {
    RabbitConsumer.readFromFile(configFile)

    configurations.configs.map(verifyOrFail)

//    configurations.configs.foreach(
//        (x) => {
//                  println("============== "+x +"============") ;
//                  scala.io.Source.fromFile(ConfigService.getFilename(x)).getLines foreach println _
//                }
//        )
  }

}
