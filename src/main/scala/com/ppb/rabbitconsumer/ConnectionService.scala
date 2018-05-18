package com.ppb.rabbitconsumer

import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.Config
import argonaut._
import com.ppb.rabbitconsumer.ConfigService._
import com.ppb.rabbitconsumer.RabbitConnection._
import com.rabbitmq.client.AMQP.BasicProperties
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

sealed trait RabbitResponse extends Product with Serializable
case object NoMoreMessages extends RabbitResponse
case class RabbitMessage(payload: Json) extends RabbitResponse

object ConnectionService {

  private val logger = LoggerFactory.getLogger(ConnectionService.getClass)

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

  def init(headers: Map[String, AnyRef])(config: Config): Cxn = {
    implicit val rabbitCxn: RabbitConnection = rabbitConnection(config)

    val queueName = readQueue(config)
    val exchangeName = readExchange(config)
    val routingKey = readRoutingKey(config)

    createQueue(queueName)
    bindQueueToExchange(queueName, exchangeName, routingKey)

    val consumeNext: () => Array[Byte] = () => {
      rabbitCxn.channel.basicGet(readQueue(config), false).getBody
    }

    def publishNext(headers: Map[String, AnyRef])(payload: String): Unit = {
      rabbitCxn.channel.basicPublish(exchangeName, routingKey, new BasicProperties.Builder().headers(headers.asJava).build(), payload.getBytes)
    }

    Cxn(
      getInputFilename(config),
      getOutputFilename(config),
      () => RabbitConnection.nextPayload(queueName)(consumeNext),
      publishNext(headers),
      () => RabbitConnection.disconnect
    )
  }

  def done(config: Config): Unit = {
    val queueName = readQueue(config)
    logger.info(s"Deleting $queueName")
    deleteQueue(queueName)(rabbitConnection(config))
  }
}
