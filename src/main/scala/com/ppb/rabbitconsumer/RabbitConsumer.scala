package com.ppb.rabbitconsumer

import argonaut._

import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.util.{Success, Try}
import scalaz.concurrent.Task
import scalaz.stream._

case class Cxn(filename: String, nextMessage: () => Try[Json], disconnect: () => Try[Unit])

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

    cxns foreach { cxn => {
      getMessages(cxn).run.run
      cxn.disconnect()
    }
    }

    logger.info(s"Done receiving $configName messages")

    logger.info(s"""When you're done testing, run "R.done("$configName") to delete the following Rabbit queues:""")
    connections.foreach { config =>
      logger.info(s"- ${config.getString("queue")}")
    }
  }

  private def getMessages(cxn: Cxn): Process[Task, Unit] = {
    Process(jsonPreamble) ++
      (receiveAll(cxn.nextMessage) map (_.spaces2) intersperse ",") ++
      Process(jsonPostamble) pipe text.utf8Encode to io.fileChunkW(cxn.filename)
  }

  def receiveAll(nextMessage: () => Try[Json]): Process0[Json] = {
    nextMessage() match {
      case Success(txt) => Process.emit(txt) ++ receiveAll(nextMessage)
      case _            => Process.halt
    }
  }
}
