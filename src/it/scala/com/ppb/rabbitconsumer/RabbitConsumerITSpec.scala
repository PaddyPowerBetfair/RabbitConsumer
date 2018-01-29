package com.ppb.rabbitconsumer

import java.io.File

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._


class RabbitConsumerITSpec extends FlatSpec with Matchers {


  it should "read docker local configuration files and process it" in {
    RabbitConsumer.local

    val configFile:File = new File("src/it/resources/local.conf")
    val config:Configurations = Configurations(configFile.getName, ConfigFactory.parseFile(configFile).getConfigList("amqp.connections").asScala.toList)
      config.configs.foreach( (x) => {
        val file:File = new File(ConfigService.getFilename(x))
        file.exists() shouldBe true
      })
    }


}
