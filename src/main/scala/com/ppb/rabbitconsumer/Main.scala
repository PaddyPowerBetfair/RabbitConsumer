package com.ppb.rabbitconsumer

import java.nio.file._

import com.rabbitmq.client.ConnectionFactory
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser._
import monix.execution.Scheduler.Implicits.global
import monix.nio.file._
import monix.reactive.Observable
import org.rogach.scallop._

import scala.concurrent.duration._

object Main extends App with LazyLogging {

  implicit val DurationValueConverter: ValueConverter[Duration] = singleArgConverter[Duration](Duration.apply)

  object Conf extends ScallopConf(args) {
    val host: ScallopOption[String] = opt[String](default = Some("localhost"), noshort = true)
    val port: ScallopOption[Int] = opt[Int](default = Some(5672), noshort = true)
    val username: ScallopOption[String] = opt[String](default = Some("guest"), noshort = true)
    val password: ScallopOption[String] = opt[String](default = Some("guest"), noshort = true)
    val useSsl: ScallopOption[Boolean] = opt[Boolean](default = Some(false), noshort = true)
    val pretty: ScallopOption[Boolean] = opt[Boolean](default = Some(false), noshort = true)
    val timeout: ScallopOption[Duration] = opt[Duration](noshort = true)
    val exchange: ScallopOption[String] = trailArg[String]("exchange")
    val routingKey: ScallopOption[String] = trailArg[String]("routingKey")
    val fileName: ScallopOption[Path] = trailArg[Path]()
    verify()
  }

  import Conf._

  val factory = new ConnectionFactory()
  factory.setHost(host())
  factory.setPort(port())
  factory.setUsername(username())
  factory.setPassword(password())
  if (useSsl()) factory.useSslProtocol()

  val connection = factory.newConnection()
  val consumer = new AsyncRabbitConsumer(connection.createChannel)
  val observable = "[\n".getBytes +: consumer.observeExchange(exchange(), routingKey()).flatMap { arr =>
    parse(new String(arr)) match {
      case Left(e) =>
        logger.warn(e.message)
        Observable.empty

      case Right(json) if json.isObject =>
        Observable.now((if (pretty()) json.spaces2 else json.noSpaces).getBytes)

      case Right(json) =>
        logger.warn(s"Json object expected, but '$json' provided")
        Observable.empty
    }
  }.intersperse(",\n".getBytes)

  val cancellable = observable.consumeWith(writeAsync(fileName(), Seq(StandardOpenOption.TRUNCATE_EXISTING))).runAsync
  logger.info(s"Started reading from AMQP exchange '${exchange()}' to the file ${fileName()}")

  cancellable.failed.foreach { e =>
    logger.error("Failed to observe", e)
  }

  def shutdown(): Unit = {
    cancellable.cancel()
    // end parameter in intersperse isn't applied because of cancelling, so we have to write it manually
    Files.write(fileName(), "\n]\n".getBytes, StandardOpenOption.APPEND)
    connection.close()
  }

  sys.ShutdownHookThread(shutdown())

  timeout.collect {
    case d: FiniteDuration => d
  }.toOption.fold {
    logger.info("No timeout is specified, to stop press Ctrl+C (SIGINT)")
  } { duration =>
    logger.info(s"Consuming will be automatically stopped after $duration")
    global.scheduleOnce(duration) {
      logger.info(s"Stopping due to timeout $duration")
      cancellable.cancel()
    }
  }

}
