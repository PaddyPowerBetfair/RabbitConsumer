package com.ppb.rabbitconsumer

import com.rabbitmq.client._
import com.typesafe.scalalogging.LazyLogging
import monix.execution.Ack.Stop
import monix.execution.Cancelable
import monix.reactive.{Observable, OverflowStrategy}

import scala.util.control.NonFatal

class AsyncRabbitConsumer(channel: Channel) extends LazyLogging {

  def observeExchange(exchange: String, routingKey: String): Observable[Array[Byte]] = {
    val queue = channel.queueDeclare().getQueue
    logger.info(s"Queue '$queue' is created")
    channel.queueBind(queue, exchange, routingKey)
    logger.info(s"Queue '$queue' is bound to exchange '$exchange' with routing key '$routingKey'")
    observeQueue(queue, {
      channel.queueUnbind(queue, exchange, routingKey)
      logger.info(s"Queue '$queue' is unbound from exchange '$exchange' with routing key '$routingKey'")
      channel.queueDelete(queue)
      logger.info(s"Queue '$queue' is deleted")
    })
  }

  def observeQueue(queue: String, onCancel: => Unit = ()): Observable[Array[Byte]] =
    Observable.create[Array[Byte]](OverflowStrategy.Unbounded) { subscriber =>
      try {
        def cancel(consumerTag: String): Unit = {
          channel.basicCancel(consumerTag)
          onCancel
        }

        val consumerTag = channel.basicConsume(queue, false, new Consumer {
          def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
            val ack = subscriber.onNext(body)
            channel.basicAck(envelope.getDeliveryTag, false)
            if (ack == Stop) cancel(consumerTag)
          }

          def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException): Unit = {
            subscriber.onError(sig)
          }

          def handleCancel(consumerTag: String): Unit = {
            subscriber.onComplete()
          }

          def handleCancelOk(consumerTag: String): Unit = handleCancel(consumerTag)

          def handleConsumeOk(consumerTag: String): Unit = ()

          def handleRecoverOk(consumerTag: String): Unit = ()
        })

        Cancelable(() => cancel(consumerTag))
      } catch {
        case NonFatal(t) =>
          subscriber.onError(t)
          Cancelable.empty
      }
    }

}
