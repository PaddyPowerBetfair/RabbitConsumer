package com.ppb.rabbitconsumer


import com.rabbitmq.client._

import argonaut._
import Argonaut._

import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scalaz.concurrent.Task
import scalaz.stream._

case class Cxn(connection: Connection, channel: com.rabbitmq.client.Channel, queueName: String)

object RabbitConsumer {
  val jsonPreamble = "{\n    \"all\": ["
  val jsonPostamble = "]}"

  private val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  def local(): Unit = all("local")

  def done(configName: String): Unit = {
    val connections = ConfigFactory.load(configName).getConfigList("amqp.connections").asScala

    connections.foreach { config =>
      val queueName    = config.getString("queue")
      val connection = ConnectionService.connectionFactory(config).newConnection()
      val channel    = connection.createChannel()
      logger.info(s"Deleting $queueName from $configName")
      channel.queueDelete(queueName)
    }
  }

  def all(configName: String): Unit = {
    val connections: immutable.Seq[Config] = ConfigFactory.load(configName).getConfigList("amqp.connections").asScala.toList

    val cxns = connections.map(ConnectionService.init)

    connections zip cxns foreach {
      case (config, cxn) =>
        val filename = config.getString("fileName").replaceFirst("^~", System.getProperty("user.home"))

        getMessages(filename)(cxn).run.run

        ConnectionService.close(cxn)
    }

    logger.info(s"Done receiving $configName messages")

    logger.info(s"""When you're done testing, run "R.done("$configName") to delete the following Rabbit queues:""")
    connections.foreach { config =>
      logger.info(s"- ${config.getString("queue")}")
    }
  }

  private def getMessages(filename: String)(cxn: Cxn): Process[Task, Unit] = {
    Process(jsonPreamble) ++
      (receiveAll(cxn) map (_.spaces2) intersperse ",") ++
      Process(jsonPostamble) pipe text.utf8Encode to io.fileChunkW(filename)
  }

  private def receiveAll(cxn: Cxn): Process0[Json] = {
    val response = Option(cxn.channel.basicGet(cxn.queueName, false))

    val json = response.flatMap { res =>
      new String(res.getBody, "UTF-8").parseOption
    }

    json match {
      case Some(txt) => Process.emit(txt) ++ receiveAll(cxn)
      case None      => Process.halt
    }
  }
}
