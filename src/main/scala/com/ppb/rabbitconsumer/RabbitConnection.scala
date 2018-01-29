package com.ppb.rabbitconsumer

import argonaut.Json
import com.rabbitmq.client._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import argonaut._
import Argonaut._
import java.io.IOException

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


sealed trait MessageParser {  def msgParser(load:Array[Byte]):Try[RabbitResponse] }

case object PlainMessageParser extends MessageParser {
  override def msgParser(load: Array[Byte]): Try[RabbitResponse] = {
      new String(load, "UTF-8") match {
        case null => Failure(new Exception("Null Payload"))
        case (text) => Success(RabbitPlainMessage(text))
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




//trait QueueSubscriber {
//  def subscribeNow(quene: String, consumer: (Array[Byte]) => Unit): Unit;
//}

//
//trait QueueSubscriber extends Consumer {
//  def messageReceived(consumerTag:String, envelop:Envelope, properties:AMQP.BasicProperties, payload:Array[Byte]): Unit;
//}
//
//case class DefaultQueueSubscriber( messageProcessor: (Array[Byte]) => Unit ) extends QueueSubscriber {
////  def messageReceived(consumerTag: String, envelop: Envelope, properties: AMQP.BasicProperties, payload: Array[Byte]): Unit = {
////    messageProcessor(payload);
////  }
//  //import com.rabbitmq.client.AMQP
//
//
//  @throws[IOException]
//  def handleDelivery(consumerTag: String, envelop: Envelope, properties: AMQP.BasicProperties, payload: Array[Byte]): Unit = {
//    messageProcessor(payload);
//  }
//
//}


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

  def nextPayload(queueName: String)(implicit rabbitConnection: RabbitConnection): RabbitResponse = {
    val response = for {
      message <- Try(rabbitConnection.nextMessage())
      json    <- asJson(message)
    } yield RabbitJsonMessage(json)

    response match {
      case Success(msg) => msg
      case Failure(th) => NoMoreMessages
    }
  }

//  def subscribe(queueName: String, consumer:Consumer) (implicit rabbitConnection: RabbitConnection): Unit = {
//              rabbitConnection.channel.basicConsume(queueName, false, consumer)
//  }


  def newNextPayload(queueName: String, messageParser: MessageParser = PlainMessageParser )(implicit rabbitConnection: RabbitConnection): RabbitResponse = {

    val response = for {
      message <- Try(rabbitConnection.nextMessage())
      rabbitResponse <- messageParser.msgParser(message)
    } yield rabbitResponse

    response match {
      case Failure(th) => NoMoreMessages
      case Success(rabbitResp) => rabbitResp
    }
  }


  // Function takes queueName, messageParser and returns a function which takes in a callback and return nothing.
  // Return function will be invoked by the consumer registering a callbackFunction
  // when message is received 'handleDelivery' is invoked which starts a future which will parse message and pass it callback function on complete
  def registerListener(queueName:String, messageParser: MessageParser = PlainMessageParser)(implicit rabbitConnection: RabbitConnection):  ((Try[RabbitResponse] => Unit) => String) = {

      val definedConsumer: (Try[RabbitResponse] => Unit) => String = (callbackFun) => {
        rabbitConnection.channel.basicConsume(queueName, false, new DefaultConsumer(rabbitConnection.channel) {

            @throws(classOf[IOException])
            override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
              Future {
                  messageParser.msgParser(body) match {
                    case Failure(th) => RabbitException(th)
                    case Success(rabbitResponse) => RabbitMessage(rabbitResponse, properties.getHeaders)
                }
              }.onComplete(callbackFun)
            } // end handle
          }) // end consume
      }
      definedConsumer
  }

  def asJson(payload: Array[Byte]): Try[Json] =
    new String(payload, "UTF-8").parse match {
      case Right(json) => Success(json)
      case Left(error) => Failure(new IllegalStateException(error))
    }
}

case class RabbitConnection(connection: Connection, channel: Channel, nextMessage: () => Array[Byte])