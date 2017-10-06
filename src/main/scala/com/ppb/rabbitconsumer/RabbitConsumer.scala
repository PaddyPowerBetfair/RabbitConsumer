package com.ppb.rabbitconsumer

import argonaut._

import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.util.{Success, Try}
import scalaz.concurrent.Task
import scalaz.stream._

case class Cxn(filename: String, nextMessage: () => Try[Json], disconnect: () => Try[Unit])

case class Configurations(name: String, config: List[Config])


object RabbitConsumer {
  val jsonPreamble = "{\n    \"all\": ["
  val jsonPostamble = "]}"

  private val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  def local(): Unit = read("local")

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

  def getConfigs(configName: String): Configurations = {
    val configs = ConfigFactory.load(configName).getConfigList("amqp.connections").asScala.toList
    Configurations(configName, configs)
  }

  val read = getConfigs _ andThen all

  private def all(c: Configurations): Unit = {
    c.config.map(ConnectionService.init) foreach { cxn => {
      getMessages(cxn).run.run
      cxn.disconnect()
    }
    }

    logger.info(s"Done receiving ${c.name} messages")

    logger.info(s"""When you're done testing, run "R.done("${c.name}") to delete the following Rabbit queues:""")
    c.config.foreach { config =>
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
