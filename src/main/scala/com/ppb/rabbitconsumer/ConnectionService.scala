package com.ppb.rabbitconsumer

import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.Config

import argonaut._
import com.ppb.rabbitconsumer.ConfigService.{getFilename, readExchange, readQueue, readRoutingKey}
import com.ppb.rabbitconsumer.RabbitConnection._
import org.slf4j.LoggerFactory

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

    val nextMessage: () => Array[Byte] = () => {
      channel.basicGet(readQueue(config), false).getBody
    }

    RabbitConnection(connection, channel, nextMessage)
  }

  def init(config: Config): Cxn = {
    implicit val rabbitConnnection: RabbitConnection = rabbitConnection(config)

    val queueName = readQueue(config)
    val exchangeName = readExchange(config)
    val routingKey = readRoutingKey(config)

    createQueue(queueName)
    bindQueueToExchange(queueName, exchangeName, routingKey)

    Cxn(getFilename(config), () => RabbitConnection.nextPayload(queueName), () => RabbitConnection.disconnect)
  }

  def done(config: Config): Unit = {
    val queueName = readQueue(config)
    logger.info(s"Deleting $queueName")
    deleteQueue(queueName)(rabbitConnection(config))
  }
}
