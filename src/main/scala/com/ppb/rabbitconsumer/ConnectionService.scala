package com.ppb.rabbitconsumer

import com.rabbitmq.client.{ConnectionFactory, GetResponse}
import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import argonaut._
import Argonaut._
import com.ppb.rabbitconsumer.ConfigService.{getFilename, readExchange, readQueue, readRoutingKey}
import com.ppb.rabbitconsumer.ConnectionService.RabbitConnection.{bindQueueToExchange, createQueue, deleteQueue}
import com.rabbitmq.client.{Channel, Connection}
import org.slf4j.LoggerFactory

object ConnectionService {

  private val logger = LoggerFactory.getLogger(ConnectionService.getClass)

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
  }

  case class RabbitConnection(connection: Connection, channel: Channel)

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

  def nextPayload(queueName: String)(implicit rabbitConnection: RabbitConnection): Try[GetResponse] =
    Try(rabbitConnection.channel.basicGet(queueName, false))

  def asJson(payload: Array[Byte]): Try[Json] =
    new String(payload, "UTF-8").parse match {
      case Right(json) => Success(json)
      case Left(error) => Failure(throw new IllegalStateException(error))
    }

  def nextMessage(queueName: String)(implicit rabbitConnection: RabbitConnection): () => Try[Json] = () =>
    nextPayload(queueName) flatMap { res => asJson(res.getBody) }

  def init(config: Config): Cxn = {
    implicit val rabbitConnnection: RabbitConnection = rabbitConnection(config)

    val queueName = readQueue(config)
    val exchangeName = readExchange(config)
    val routingKey = readRoutingKey(config)

    createQueue(queueName)
    bindQueueToExchange(queueName, exchangeName, routingKey)

    Cxn(getFilename(config), nextMessage(queueName), () => RabbitConnection.disconnect)
  }

  def done(config: Config): Unit = {
    val queueName = readQueue(config)
    logger.info(s"Deleting $queueName")
    deleteQueue(queueName)(rabbitConnection(config))
  }
}
