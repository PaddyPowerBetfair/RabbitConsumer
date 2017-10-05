package com.ppb.rabbitconsumer

import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.Config

import scala.util.Try
import scala.collection.JavaConverters._

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


  def init(config: Config): Cxn = {
    val exchange     = config.getString("exchangeName")
    val queueName    = config.getString("queue")
    val routingKey   = config.getString("routingKey")

    val connection = connectionFactory(config).newConnection()
    val channel    = connection.createChannel()

    channel.exchangeDeclarePassive(exchange)

    channel.queueDeclare(queueName, true, false, false, Map.empty[String, AnyRef].asJava).getQueue
    channel.queueBind(queueName, exchange, routingKey)

    Cxn(connection, channel, queueName)
  }

  def close(cxn: Cxn): Try[Unit] = {
    for {
      _ <- Try(cxn.channel.close())
      _ <- Try(cxn.connection.close())
    } yield ()
  }
}
