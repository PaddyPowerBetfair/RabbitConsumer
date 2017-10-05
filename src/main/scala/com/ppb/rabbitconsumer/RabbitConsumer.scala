package com.ppb.rabbitconsumer


import com.rabbitmq.client._

import scalaz._
import Scalaz._
import argonaut._
import Argonaut._

import scala.collection.JavaConverters._
import scala.util.Try
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scalaz.concurrent.Task
import scalaz.stream._



case class Cxn(connection: Connection, channel: com.rabbitmq.client.Channel, queueName: String)

object RabbitConsumer {

  val jsonPreamble = "{\n    \"all\": ["
  val jsonPostamble = "]}"

  val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  private def connectionFactory(config: Config): ConnectionFactory = {
    val cxnFactory = new ConnectionFactory()

    cxnFactory.setHost(config.getString("ip"))
    cxnFactory.setPort(config.getInt("port"))
    cxnFactory.setUsername(config.getString("user"))
    cxnFactory.setPassword(config.getString("password"))

    if (config.getBoolean("useSSL")) cxnFactory.useSslProtocol()

    cxnFactory
  }


  private def init(config: Config): Cxn = {
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

  private def close(cxn: Cxn) = {
    for {
      _ <- Try(cxn.channel.close())
      _ <- Try(cxn.connection.close())
    } yield ()
  }

  def local() = all("local")
  def uat() = all("uat")
  def oat() = all("oat")

  def done(configName: String): Unit = {
    val connections = ConfigFactory.load(configName).getConfigList("amqp.connections").asScala

    connections.foreach { config =>
      val queueName    = config.getString("queue")
      val connection = connectionFactory(config).newConnection()
      val channel    = connection.createChannel()
      println(s"Deleting $queueName from $configName")
      channel.queueDelete(queueName)
    }
  }

  def all(configName: String): Unit = {
    val connections = ConfigFactory.load(configName).getConfigList("amqp.connections").asScala

    val cxns = connections.map(init)

    connections zip cxns foreach {
      case (config, cxn) =>
        val filename = config.getString("fileName").replaceFirst("^~", System.getProperty("user.home"))

        getMessages(filename)(cxn).run.run

        close(cxn)
    }

    println(s"Done receiving $configName messages")

    println(s"""When you're done testing, run "R.done("$configName") to delete the following Rabbit queues:""")
    connections.foreach { config =>
      println(s"- ${config.getString("queue")}")
    }
  }

  private def getMessages(filename: String)(cxn: Cxn): Process[Task, Unit] = {
    Process(jsonPreamble) ++ (receiveAll(cxn) map (_.spaces2) intersperse ",") ++ Process(jsonPostamble) pipe text.utf8Encode to io.fileChunkW(filename)
  }

  private def receiveAll(cxn: Cxn): Process0[Json] = {
    val response = Option(cxn.channel.basicGet(cxn.queueName, false))

    val json = response.flatMap { res => {
      val msgBody = new String(res.getBody, "UTF-8")

      // cxn.channel.basicAck(res.getEnvelope.getDeliveryTag, false)

      msgBody.parseOption
    }
    }

    json match {
      case Some(txt) => Process.emit(txt) ++ receiveAll(cxn)
      case None      => Process.halt
    }
  }
}
