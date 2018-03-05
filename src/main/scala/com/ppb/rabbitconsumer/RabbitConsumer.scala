package com.ppb.rabbitconsumer

import java.io.File
import java.util.Date

import argonaut._

import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import scodec.bits.ByteVector

import scala.util.Try
import scalaz.concurrent.Task
import scalaz.stream.{Sink, _}


object RabbitConsumerAlgebra {

  sealed trait RabbitResponse extends Product with Serializable
  case object NoMoreMessages extends RabbitResponse
  case class RabbitException(throwable:Throwable) extends RabbitResponse

  /**
    * Message Format used for representing Json type message payload
    * @param payload
    */
  case class RabbitJsonMessage(payload: Json) extends RabbitResponse

  /**
    * Message Format used to represent Simple String type message payload
    * @param plainPayload
    */
  case class RabbitPlainMessage(plainPayload: String) extends RabbitResponse

  /**
    * Type OnReceive accepts timestamp as Long , queueName as String, RabbitResponse as try[RabbitResponse], and header as Map.
    * It Returns a Unit
    */
  type OnReceive = (Date, String, Try[RabbitResponse], Map[String, AnyRef]) => Unit
 // type ProcessMessage = (SingleResponseConnection) => Process[Task, Unit]
 // type MessageStreaming = (Any) => Process[Task, Unit]

  /**
    * Class for holding Message reader and disconnect function from queue or exchange.
    * //@param stream     : stream Sink where response will be streamed
    * @param nextMessage  : Function which will read queue and return Rabbit Response
    * @param disconnect   : Function to disconnect from queue
    */
//  case class SingleResponseConnection(filename: String, nextMessage: () => RabbitResponse, disconnect: () => Try[Unit])
  case class SingleResponseConnection(nextMessage: () => RabbitResponse, disconnect: () => Try[Unit], stream: Sink[Task, ByteVector] = io.stdOutBytes)

  /**
    * Class for holding subscription to the queue
    * @param subscription : of Type 'OnReceive' function holder, will be called when message arrives and consumed
    * @param disconnect : disconnect from queue
    */
  case class SubscriptionConnection(subscription:OnReceive => String, disconnect: () => Try[Unit], fileName:Option[String] = None)

  /**
    * class for holding configuration
    * @param name : Name of config
    * @param configs : List of Configuration of type Config
    */
  case class Configurations(name: String, configs: List[Config])
}



object RabbitConsumer {

  import RabbitConsumerAlgebra._

  //val jsonPreamble = "{\n    \"all\": ["
  //val jsonPostamble = "]}"

  private val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  def local(): Unit = readFromResource("local")

  def done(configName: String): Unit =
    getConfigFromResource(configName).configs foreach ConnectionService.done


  def getConfigFromResource(configName: String): Configurations = {
    val configs = ConfigFactory.load(configName).getConfigList("amqp.connections").asScala.toList
    Configurations(configName, configs)
  }


  def getConfigsFromFile(configFile: String): Configurations =
    Configurations(configFile, ConfigFactory.parseFile(new File(configFile)).getConfigList("amqp.connections").asScala.toList)


  val getMessagesPerConnection: (SingleResponseConnection) => Process[Task, Unit] = (cxn) => {
   //getMessagesWithPreamble(cxn.nextMessage).toSource pipe text.utf8Encode to cxn.stream
    getMessages(cxn.nextMessage).toSource pipe text.utf8Encode to cxn.stream
  }


  val readFromResource: (String) => Unit =   getConfigFromResource _ andThen consumeMessages(ConnectionService.createConnection(_), getMessagesPerConnection)
  val readFromFile: (String) => Unit =   getConfigsFromFile _ andThen consumeMessages(ConnectionService.createConnection(_), getMessagesPerConnection)


  def consumeMessages(getCxn: Config => SingleResponseConnection, fxMessages: (SingleResponseConnection) => Process[Task, Unit])(c: Configurations): Unit = {
    c.configs.map(getCxn) foreach { cxn => {
      fxMessages(cxn).run.run
      cxn.disconnect()
     }
    }
  }

  def subscribeMessages(createSubscriptionConnection: Config => SubscriptionConnection, onReceiveFun:Config => OnReceive)(c: Configurations): Unit = {
      c.configs.map(config =>  createSubscriptionConnection(config).subscription(onReceiveFun(config)))

  }




  @Deprecated
  def getMessagesWithPreamble(nextMessage: () => RabbitResponse): Process0[String] = getMessages(nextMessage)
           // Process(jsonPreamble) ++ getMessages(nextMessage) ++ Process(jsonPostamble)


  def getMessages(nextMessage: () => RabbitResponse): Process0[String] =
      receiveAllAny(nextMessage) match  {
        case ss:Process0[String] => ss intersperse("\n")
        case kk:Process0[Json] => kk map(_.spaces2) intersperse ","
      }

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
      case RabbitPlainMessage(plainPayload) =>  Process.emit(plainPayload) ++ receiveAllAny(nextMessage)
      case _ => Process.halt
    }
}
