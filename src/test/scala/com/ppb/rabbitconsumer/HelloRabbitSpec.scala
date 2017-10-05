package com.ppb.rabbitconsumer

import org.scalatest.{BeforeAndAfter, FunSuite}
import org.slf4j.LoggerFactory



class HelloRabbitSpec extends FunSuite with BeforeAndAfter {

  private val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  before {
    logger.info(" Inside Before ")
  }

  after {
    logger.info(" Inside Before ")
  }


  test("Hello Rabbit ") {

    logger.info(" Calling Hello Rabbit  ")
     assert( HelloRabbit.checkRabbit("HelloRabbit")  )
  }


}
