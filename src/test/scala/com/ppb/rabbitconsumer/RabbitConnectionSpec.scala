package com.ppb.rabbitconsumer

import com.rabbitmq.client.AMQP.Queue.{BindOk, DeleteOk}
import com.rabbitmq.client.impl.AMQImpl.Queue.DeclareOk
import com.rabbitmq.client.{Channel, Connection}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}
import org.mockito.Mockito._

import scala.collection.JavaConverters._

class RabbitConnectionSpec extends FlatSpec with Matchers with MockitoSugar {

  behavior of "RabbitConnection"

  it should "create an exchange on the RabbitMq Broker" in {
    val connection = mock[Connection]
    val channel = mock[Channel]
    val nextMessage = (Int) => Array.empty[Byte]

    val rabbitConnection = RabbitConnection(connection, channel, nextMessage)
    RabbitConnection.createExchange("exchange")(rabbitConnection)

    verify(channel, times(1)).exchangeDeclarePassive("exchange")
  }

  it should "create a queue on the RabbitMq Broker" in {
    val connection = mock[Connection]
    val channel = mock[Channel]
    val nextMessage = (Int) => Array.empty[Byte]

    when(channel.queueDeclare("queue", true, false, false, Map.empty[String, AnyRef].asJava)).thenReturn(mock[DeclareOk])
    val rabbitConnection = RabbitConnection(connection, channel, nextMessage)
    RabbitConnection.createQueue("queue")(rabbitConnection)
  }

  it should "bind a queue to an exchange on the RabbitMq Broker" in {
    val connection = mock[Connection]
    val channel = mock[Channel]
    val nextMessage = (Int) => Array.empty[Byte]

    when(channel.queueBind("queue", "exchange", "routingKey")).thenReturn(mock[BindOk])
    val rabbitConnection = RabbitConnection(connection, channel, nextMessage)
    RabbitConnection.bindQueueToExchange("queue", "exchange", "routingKey")(rabbitConnection)
  }

  it should "delete a queue on the RabbitMq Broker" in {
    val connection = mock[Connection]
    val channel = mock[Channel]
    val nextMessage = (Int) => Array.empty[Byte]

    when(channel.queueDelete("queue")).thenReturn(mock[DeleteOk])
    val rabbitConnection = RabbitConnection(connection, channel, nextMessage)
    RabbitConnection.deleteQueue("queue")(rabbitConnection)
  }


  it should "convert valid json into a json object" in {
    val input = """{"key":"value"}"""
    RabbitConnection.asJson(input.getBytes).get.toString() should be (input)
  }

  it should "convert invalid json into a json object" in {
    val input = "not valid json"
    RabbitConnection.asJson(input.getBytes) shouldBe a[Failure[_]]
  }

  it should "disconnect from RabbitMQ Broker" in {
    val connection = mock[Connection]
    val channel = mock[Channel]
    val nextMessage = (Int) => Array.empty[Byte]

    val rabbitConnection = RabbitConnection(connection, channel, nextMessage(1))
    RabbitConnection.disconnect(rabbitConnection) should be (Success(()))

    verify(channel, times(1)).close()
    verify(connection, times(1)).close()
  }

  it should "return a valid Message object from the RabbitMq Broker when a payload is available" in {
    val connection = mock[Connection]
    val channel = mock[Channel]

    val nextMessage = (Int) => """{ "key": "value" }""".getBytes

    val rabbitConnection = RabbitConnection(connection, channel, nextMessage(1))

    val message: RabbitResponse = RabbitConnection.nextPayload("someQueue")(rabbitConnection)
    message shouldBe a[RabbitMessage]
  }

  it should "return a NoMoreMessages object from the RabbitMq Broker when no payload is available" in {
    val connection = mock[Connection]
    val channel = mock[Channel]

    val nextMessage = (Int) => throw new IllegalStateException("No more messages")

    val rabbitConnection = RabbitConnection(connection, channel, nextMessage(1))

    val message: RabbitResponse = RabbitConnection.nextPayload("someQueue")(rabbitConnection)
    message shouldBe NoMoreMessages
  }

}
