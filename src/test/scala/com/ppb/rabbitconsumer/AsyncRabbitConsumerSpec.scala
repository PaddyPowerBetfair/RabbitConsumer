package com.ppb.rabbitconsumer

import com.rabbitmq.client.AMQP._
import com.rabbitmq.client.{Channel, Envelope}
import monix.execution.schedulers.TestScheduler
import monix.reactive.Consumer
import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec

class AsyncRabbitConsumerSpec extends FlatSpec with MockFactory {

  implicit val scheduler: TestScheduler = TestScheduler()
  val channel: Channel = mock[Channel]
  val consumer = new AsyncRabbitConsumer(channel)

  "AsyncRabbitConsumer" should "observe queue" in {
    val observable = consumer.observeQueue("testQueue")

    (channel.basicConsume(_: String, _: Boolean, _: com.rabbitmq.client.Consumer)).expects("testQueue", false, *).onCall { (_, _, consumer) =>
      consumer.handleDelivery("consumerTag", new Envelope(12345, false, "testExchange", "testRoutingKey"), new BasicProperties, "hello".getBytes)
      "consumerTag"
    }
    (channel.basicAck _).expects(12345, false)
    val cancellable = observable.consumeWith(Consumer.head).runAsync

    (channel.basicCancel _).expects("consumerTag")
    cancellable.cancel()
  }

  it should "observe exchange" in {
    (() => channel.queueDeclare()).expects().returning(new Queue.DeclareOk.Builder().queue("testQueue").build())
    (channel.queueBind(_: String, _: String, _: String)).expects("testQueue", "testExchange", "testRoutingKey").returning(new Queue.BindOk.Builder().build())

    val observable = consumer.observeExchange("testExchange", "testRoutingKey")

    (channel.basicConsume(_: String, _: Boolean, _: com.rabbitmq.client.Consumer)).expects("testQueue", false, *).onCall { (_, _, consumer) =>
      consumer.handleDelivery("consumerTag", new Envelope(12345, false, "testExchange", "testRoutingKey"), new BasicProperties, "hello".getBytes)
      "consumerTag"
    }
    (channel.basicAck _).expects(12345, false)
    val cancellable = observable.consumeWith(Consumer.head).runAsync

    (channel.basicCancel _).expects("consumerTag")
    (channel.queueUnbind(_: String, _: String, _: String)).expects("testQueue", "testExchange", "testRoutingKey").returning(new Queue.UnbindOk.Builder().build())
    (channel.queueDelete(_: String)).expects("testQueue").returning(new Queue.DeleteOk.Builder().build())
    cancellable.cancel()
  }

}
