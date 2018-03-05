package com.ppb.rabbitconsumer

import java.io.IOException

import argonaut.Argonaut._
import argonaut.{Json, _}
import com.ppb.rabbitconsumer.RabbitConsumerAlgebra._
import com.rabbitmq.client._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


sealed trait MessageParser {  def msgParser(load:Array[Byte]):Try[RabbitResponse] }

case object PlainMessageParser extends MessageParser {
  override def msgParser(load: Array[Byte]): Try[RabbitResponse] = {
    Option(new String(load, "UTF-8")) match {
      case None => Failure(new IllegalArgumentException("payload is null"))
      case Some(text) => Success(RabbitPlainMessage(text))
    }
  }
}

case object JSONMessageParser extends MessageParser {
  override def msgParser(load: Array[Byte]): Try[RabbitResponse] = {
    new String(load, "UTF-8").parse match {
      case Right(json) => Success(RabbitJsonMessage(json))
      case Left(error) => Failure(new IllegalStateException(error))
    }
  }
}





object RabbitConnection {
  def disconnect(implicit rabbitConnection: RabbitConnection): Try[Unit] = {
    for {
      _ <- Try(rabbitConnection.channel.close())
      _ <- Try(rabbitConnection.connection.close())
    } yield ()
  }

  def createExchange(exchange: String)(implicit rabbitConnection: RabbitConnection): Unit =
    rabbitConnection.channel.exchangeDeclarePassive(exchange)

  def createQueue(queueName: String)(implicit rabbitConnection: RabbitConnection): Unit =
    rabbitConnection.channel.queueDeclare(queueName, true, false, false, Map.empty[String, AnyRef].asJava)

  def bindQueueToExchange(queueName: String, exchange: String, routingKey: String)(implicit rabbitConnection: RabbitConnection): Unit =
    rabbitConnection.channel.queueBind(queueName, exchange, routingKey)

  def deleteQueue(queueName: String)(implicit rabbitConnection: RabbitConnection): Unit =
    rabbitConnection.channel.queueDelete(queueName)

  @Deprecated
  def nextPayload(queueName: String)(implicit rabbitConnection: RabbitConnection): RabbitResponse = {
     readNextPayload(queueName , JSONMessageParser)
  }

  def readNextPayload(queueName: String, messageParser: MessageParser = PlainMessageParser )(implicit rabbitConnection: RabbitConnection): RabbitResponse = {

    val response = for {
      message <- Try(rabbitConnection.nextMessage())
      rabbitResponse <- messageParser.msgParser(message)
    } yield rabbitResponse

    response match {
      case Failure(th) => NoMoreMessages
      case Success(rabbitResp) => rabbitResp
    }
  }

  implicit class ScalaHeaderProperties(properties: BasicProperties) {
    def headerAsScalaMap: Map[String, AnyRef] = {
      Option(properties.getHeaders) map { props =>
        props.asScala.toMap[String, AnyRef]
      } getOrElse Map.empty[String, AnyRef]
    }
  }

  /**
    * Function to register to queue for message notification. This method will subscribe to queue, will consume when
    * message arrives in queue
    * @param queueName
    * @param messageParser  : Custom Parser of type MessageParse
    * @param rabbitConnection
    * @return : A function Holder of type parameter 'OnReceive' returns String.
    */
  def registerListener(queueName:String, messageParser: MessageParser = PlainMessageParser)(implicit rabbitConnection: RabbitConnection): (OnReceive) => String = {

      val definedConsumer:OnReceive => String = (callbackFun) => {
        rabbitConnection.channel.basicConsume(queueName, false, new DefaultConsumer(rabbitConnection.channel) {

          @throws(classOf[IOException])
          override def handleDelivery(consumerTag: String,
                                      envelope: Envelope,
                                      properties: AMQP.BasicProperties,
                                      body: Array[Byte]): Unit = {

              callbackFun(properties.getTimestamp, queueName, messageParser.msgParser(body), properties.headerAsScalaMap)
            } // end handle
          }) // end define consume
      }
      definedConsumer
  }


//  def publish(queueName:String, routingKey:String, message:String)(implicit  rabbitConnection: RabbitConnection):Unit = {
//    rabbitConnection.channel.basicPublish("", queueName, null, message.getBytes())
//  }

  def asJson(payload: Array[Byte]): Try[Json] =
    new String(payload, "UTF-8").parse match {
      case Right(json) => Success(json)
      case Left(error) => Failure(new IllegalStateException(error))
    }
}

case class RabbitConnection(connection: Connection, channel: Channel, nextMessage: () => Array[Byte])