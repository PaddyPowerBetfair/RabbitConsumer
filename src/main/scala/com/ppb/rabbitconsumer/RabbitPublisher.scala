package com.ppb.rabbitconsumer

import com.ppb.rabbitconsumer.RabbitConsumer.getConfigs

import scalaz.concurrent.Task
import scalaz.stream.{Sink, io, sink}

object RabbitPublisher {

  import CsvRow._
  import JsonWriterInstances._
  import JsonWriterSyntax._

  private val publisher: Cxn => Sink[Task, Option[CsvRow]] = cxn => sink.lift[Task, Option[CsvRow]] {
    case Some(csvRow) => Task.delay(cxn.publish(csvRow.toJson))
    case _            => Task.delay()
  }

  private val publishMessages: Cxn => Unit = cxn => {
    io.linesR(cxn.inputFilename)
      .drop(1)
      .map(toCsvRow)
      .to(publisher(cxn)).run.run
  }

  def publish(configName: String, headers: String*): Unit = {
    getConfigs(configName).configs
      .map(ConnectionService.init(AmqpHeader.toHeaders(headers.toList)))
      .foreach(publishMessages)
  }
}
