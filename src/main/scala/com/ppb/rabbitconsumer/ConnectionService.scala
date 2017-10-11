package com.ppb.rabbitconsumer

import com.rabbitmq.client.{ConnectionFactory, GetResponse}
import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import argonaut._
import Argonaut._

import com.rabbitmq.client.{Channel, Connection}

object ConnectionService {

  def connectionFactory(config: Config): ConnectionFactory = {
    val cxnFactory = new ConnectionFactory()

    cxnFactory.setHost(config.getString("ip"))
    cxnFactory.setPort(config.getInt("port"))
    cxnFactory.setUsername(config.getString("user"))
    cxnFactory.setPassword(config.getString("password"))

    if (config.getBoolean("useSSL")) cxnFactory.useSslProtocol()

    cxnFactory
  }


  def rabbitConnection(config: Config): RabbitConnection = {
    val connection = connectionFactory(config).newConnection()
    val channel    = connection.createChannel()

    RabbitConnection(connection, channel)
  }

  case class RabbitConnection(connection: Connection, channel: Channel)

  def readExchange(config: Config): String = config.getString("exchangeName")
  def readQueue(config: Config): String = config.getString("queue")
  def readRoutingKey(config: Config): String = config.getString("routingKey")
  def getFilename(config: Config): String = config.getString("fileName").replaceFirst("^~", System.getProperty("user.home"))

  def createExchange(rabbitConnection: RabbitConnection) : String => Unit = exchange =>
    rabbitConnection.channel.exchangeDeclarePassive(exchange)

  def createQueue(rabbitConnection: RabbitConnection): String => Unit = queueName =>
    rabbitConnection.channel.queueDeclare(queueName, true, false, false, Map.empty[String, AnyRef].asJava).getQueue

  def bindQueueToExchange(rabbitConnection: RabbitConnection)(queueName: String, exchange: String, routingKey: String): Unit =
    rabbitConnection.channel.queueBind(queueName, exchange, routingKey)

  def disconnect(rabbitConnection: RabbitConnection): () => Try[Unit] = () => for {
      _ <- Try(rabbitConnection.channel.close())
      _ <- Try(rabbitConnection.connection.close())
    } yield ()

  def nextMessage(rabbitConnection: RabbitConnection)(queueName: String): () => Try[Json] = () => {
    val response: Try[GetResponse] = Try(rabbitConnection.channel.basicGet(queueName, false))

    response flatMap { res =>
      new String(res.getBody, "UTF-8").parse match {
        case Right(json) => Success(json)
        case Left(error) => Failure(throw new IllegalStateException(error))
      }
    }
  }

  def init(config: Config): Cxn = {
    val queueName = readQueue(config)
    val rabbitConnnection = rabbitConnection(config)

    Cxn(getFilename(config), nextMessage(rabbitConnnection)(queueName), disconnect(rabbitConnnection))
  }
}
