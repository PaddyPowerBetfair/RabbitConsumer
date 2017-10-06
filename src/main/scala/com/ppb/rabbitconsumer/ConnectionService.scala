package com.ppb.rabbitconsumer

import com.rabbitmq.client.{ConnectionFactory, GetResponse}
import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import argonaut._
import Argonaut._

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

    val disconnect: () => Try[Unit] = () => for {
      _ <- Try(channel.close())
      _ <- Try(connection.close())
    } yield ()

    val nextMessage: () => Try[Json] = () => {
      val response: Try[GetResponse] = Try(channel.basicGet(queueName, false))

      response flatMap { res =>
        new String(res.getBody, "UTF-8").parse match {
          case Right(json) => Success(json)
          case Left(error) => Failure(throw new IllegalStateException(error))
        }
      }
    }

    val filename: String = config.getString("fileName").replaceFirst("^~", System.getProperty("user.home"))

    Cxn(filename, nextMessage, disconnect)
  }
}
