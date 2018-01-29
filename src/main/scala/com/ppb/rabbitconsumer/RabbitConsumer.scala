package com.ppb.rabbitconsumer

import argonaut._
import com.rabbitmq.client.Consumer

import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.util.Try
import scalaz.concurrent.Task
import scalaz.stream._

case class Cxn(filename: String, nextMessage: () => RabbitResponse, disconnect: () => Try[Unit])
case class Sxn(subscription:((Try[RabbitResponse] => Unit) => String), disconnect: () => Try[Unit])



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
    getMessagesWithPreamble(cxn.nextMessage).toSource pipe text.utf8Encode to io.fileChunkW(cxn.filename)

  val read: (String) => Unit =  getConfigs _ andThen consumeMessages(ConnectionService.init, getMessagesPerConnection)

  def consumeMessages(getCxn: Config => Cxn, fxMessages: Cxn => Process[Task, Unit])(c: Configurations): Unit = {
    c.configs.map(getCxn) foreach { cxn => {
      fxMessages(cxn).run.run
      cxn.disconnect()
    }
    }

    logger.info(s"Done receiving ${c.name} messages")

    //logger.info(s"""When you're done testing, run "R.done("${c.name}") to delete the following Rabbit queues:""")
    //c.configs.foreach { config =>
     // logger.info(s"- ${config.getString("queue")}")
   // }
  }

  def subscribeMessages(getSxn: Config => Sxn, callbackFunction:(Try[RabbitResponse]=>Unit))(c: Configurations): Unit = {
      c.configs.map(getSxn) foreach { sxn => {
        logger.info("Subscribing , please wait .....")
        sxn.subscription(callbackFunction)
        }
      }
  }



  def getMessagesWithPreamble(nextMessage: () => RabbitResponse): Process0[String] =
            Process(jsonPreamble) ++ getMessages(nextMessage) ++ Process(jsonPostamble)


  def getMessages(nextMessage: () => RabbitResponse): Process0[String] =
      (receiveAllAny(nextMessage) match  {
        case ss:Process0[String] => ss intersperse("\n")
        case kk:Process0[Json] => kk map(_.spaces2) intersperse ","
      })

//
//    Process(jsonPreamble) ++
//      (receiveAll(nextMessage) map (_.spaces2) intersperse ",") ++
//      Process(jsonPostamble)

//  def receiveAll(nextMessage: () => RabbitResponse): Process0[Json] =
//    nextMessage() match {
//      case RabbitMessage(json) => Process.emit(json) ++ receiveAll(nextMessage)
//      case NoMoreMessages => Process.halt
//    }

  def receiveAllAny(nextMessage: () => RabbitResponse): Process0[Any] =
    nextMessage() match {
      case RabbitJsonMessage(json) => Process.emit(json) ++ receiveAllAny(nextMessage)
      case NoMoreMessages => Process.halt
      case RabbitPlainMessage(plainPayload) =>  Process.emit(plainPayload) ++ receiveAllAny(nextMessage)
    }
}
