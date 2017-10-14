package com.ppb.rabbitconsumer

import argonaut.Json
import com.rabbitmq.client.{Channel, Connection}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import argonaut._
import Argonaut._

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
    rabbitConnection.channel.queueDeclare(queueName, true, false, false, Map.empty[String, AnyRef].asJava).getQueue

  def bindQueueToExchange(queueName: String, exchange: String, routingKey: String)(implicit rabbitConnection: RabbitConnection): Unit =
    rabbitConnection.channel.queueBind(queueName, exchange, routingKey)

  def deleteQueue(queueName: String)(implicit rabbitConnection: RabbitConnection): Unit =
    rabbitConnection.channel.queueDelete(queueName)

  def nextPayload(queueName: String)(implicit rabbitConnection: RabbitConnection): RabbitResponse = {
    val response = for {
      message <- Try(rabbitConnection.nextMessage())
      json    <- asJson(message)
    } yield RabbitMessage(json)

    response match {
      case Success(msg) => msg
      case Failure(th) => NoMoreMessages
    }
  }

  def asJson(payload: Array[Byte]): Try[Json] =
    new String(payload, "UTF-8").parse match {
      case Right(json) => Success(json)
      case Left(error) => Failure(new IllegalStateException(error))
    }
}

case class RabbitConnection(connection: Connection, channel: Channel, nextMessage: () => Array[Byte])


