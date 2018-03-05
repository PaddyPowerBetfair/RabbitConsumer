package com.ppb.rabbitconsumer

import com.ppb.rabbitconsumer.ConfigService.{getFilename, readExchange, readQueue, readRoutingKey}
import com.ppb.rabbitconsumer.RabbitConnection._
import com.ppb.rabbitconsumer.RabbitConsumerAlgebra.{SingleResponseConnection, SubscriptionConnection}
import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import scodec.bits.ByteVector

import scalaz.Sink
import scalaz.concurrent.Task
import scalaz.stream.{Sink, io, sink}



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


  def createConnection(config: Config) :  SingleResponseConnection = {
    implicit val rabbitConnnection: RabbitConnection = rabbitConnection(config)
    val queueName = readQueue(config)
    val exchangeName = readExchange(config)
    val routingKey = readRoutingKey(config)
    bindQueueToExchange(queueName, exchangeName, routingKey)

   val sink = Option(getFilename(config)) map {
      fileNameDefined => io.fileChunkW(fileNameDefined)
    } getOrElse io.stdOutBytes

    SingleResponseConnection(() => RabbitConnection.readNextPayload(queueName), () => RabbitConnection.disconnect, sink)
  }






  //  def init(config: Config, createQueue:Boolean = false, sink:Sink[Task, ByteVector]): SingleResponseConnection = {
//    createConnection(config, createQueue, sink)
//  }


  def subscribe(config: Config, messageParser:MessageParser = PlainMessageParser): SubscriptionConnection = {
    implicit val rabbitConnnection: RabbitConnection = rabbitConnection(config)

    val queueName = readQueue(config)
    val exchangeName = readExchange(config)
    val routingKey = readRoutingKey(config)
    val fileName = getFilename(config)

    bindQueueToExchange(queueName, exchangeName, routingKey)



    println(" Created a subscription with "+ queueName + "   " +exchangeName+ "  "+ routingKey )

   SubscriptionConnection(RabbitConnection.registerListener(queueName, messageParser), ()=>RabbitConnection.disconnect, Option(fileName))


  }




  def done(config: Config): Unit = {
    val queueName = readQueue(config)
    logger.info(s"Deleting $queueName")
    deleteQueue(queueName)(rabbitConnection(config))
  }
}
