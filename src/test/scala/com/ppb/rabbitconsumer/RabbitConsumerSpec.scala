package com.ppb.rabbitconsumer

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.slf4j.LoggerFactory


class RabbitConsumerSpec extends FunSuite with BeforeAndAfter {

  private val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  before {
    logger.info(" Inside Before ")
  }

  after {
    logger.info(" Inside Before ")
  }


  test("Rabbit Consumer") {
    logger.error(" Rebbit Consumer Not Yet Implemented ")

  }


}
