package com.ppb.rabbitconsumer

import argonaut._
import Argonaut._
import com.typesafe.config.Config
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import scalaz.concurrent.Task
import scalaz.stream.Process
import org.mockito.Mockito._

import scala.util.Success


class RabbitConsumerSpec extends FlatSpec with Matchers with MockitoSugar {

  private val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  behavior of "RabbitConsumer"

  it should "receive all messages" in new RabbitConsumerFixture {
    val message = RabbitConsumer.receiveAllAny(receiveOneMessage).toSource.runLog.run
    message should have size 1
  }

  it should "read configuration files" in {
    val config: Configurations = RabbitConsumer.getConfigs("local")
    config.name should be ("local")
    config.configs should have size 2
  }

  it should "consume messages" in {

    trait MyMock {
      def iWasCalled(): Unit
      def soWasI(): Unit
    }

    val myMock = mock[MyMock]
    val getCxn: Config => Cxn = _ => Cxn("", () => NoMoreMessages, () => { myMock.soWasI(); Success(()) })

    val getMessages: Cxn => Process[Task, Unit] = _ => {
      myMock.iWasCalled()
      Process(()).toSource
    }

    val configs: Configurations = RabbitConsumer.getConfigs("local")
    RabbitConsumer.consumeMessages(getCxn, getMessages)(configs)

    verify(myMock, times(2)).iWasCalled()
    verify(myMock, times(2)).soWasI()
  }

 }

trait RabbitConsumerFixture {

  var times = 0

  val receiveOneMessage: () => RabbitResponse = () =>
    if (times == 0) {
      times = 1
      RabbitJsonMessage("".asJson)
    } else {
      NoMoreMessages
    }
}
