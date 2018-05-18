package com.ppb.rabbitconsumer

import argonaut._

import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.util.Try
import scalaz.concurrent.Task
import scalaz.stream._

case class Cxn(inputFilename: String, outputFilename: String, nextMessage: () => RabbitResponse, publish: String => Unit, disconnect: () => Try[Unit])

case class Configurations(name: String, configs: List[Config])

object RabbitConsumer {
  val jsonPreamble = "{\n    \"all\": ["
  val jsonPostamble = "]}"

  private val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  def local(): Unit = read("local")

  def done(configName: String): Unit =
    getConfigs(configName).configs foreach ConnectionService.done


  def getConfigs(configName: String): Configurations = {
    val configs = ConfigFactory.load(configName).getConfigList("amqp.connections").asScala.toList
    Configurations(configName, configs)
  }

  val getMessagesPerConnection: Cxn => Process[Task, Unit] = cxn =>
    getMessages(cxn.nextMessage).toSource pipe text.utf8Encode to io.fileChunkW(cxn.outputFilename)

  val read: (String) => Unit =  getConfigs _ andThen consumeMessages(ConnectionService.init(Map.empty), getMessagesPerConnection)

  def consumeMessages(getCxn: Config => Cxn, getMessages: Cxn => Process[Task, Unit])(c: Configurations): Unit = {
    c.configs.map(getCxn) foreach { cxn => {
      getMessages(cxn).run.run
      cxn.disconnect()
    }
    }

    logger.info(s"Done receiving ${c.name} messages")

    logger.info(s"""When you're done testing, run "R.done("${c.name}") to delete the following Rabbit queues:""")
    c.configs.foreach { config =>
      logger.info(s"- ${config.getString("queue")}")
    }
  }

  private def getMessages(nextMessage: () => RabbitResponse): Process0[String] =
    Process(jsonPreamble) ++
      (receiveAll(nextMessage) map (_.spaces2) intersperse ",") ++
      Process(jsonPostamble)


  def receiveAll(nextMessage: () => RabbitResponse): Process0[Json] =
    nextMessage() match {
      case RabbitMessage(json) => Process.emit(json) ++ receiveAll(nextMessage)
      case NoMoreMessages      => Process.halt
    }
}
