package com.ppb.rabbitconsumer
import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfter, FlatSpec, FunSuite}

import scala.collection.JavaConverters._
import scala.collection.immutable

class ConnectionServiceSpec extends FlatSpec    {

  "Test Connection Service is " should "have IP,PORT,USERNAME,PASSWORD" in  new ConnectionServiceFixture {

    val connectionFactory:ConnectionFactory = ConnectionService.connectionFactory(config);
    assert(connectionFactory != null)
    assert(connectionFactory.getHost == config.getString("ip"), " IP Address do not match")
    assert(connectionFactory.getPort == config.getInt("port"), " PORT do not match")
    assert(connectionFactory.getUsername == config.getString("user"), " USER do not match")
    assert(connectionFactory.getPassword == config.getString("password") , " PASSWORD do not match")
    assert(!connectionFactory.isSSL, " SSL Is Not Enabled")

  }
}

trait ConnectionServiceFixture {

  val config:Config = {

    val connections: immutable.Seq[Config] = ConfigFactory.load("local").getConfigList("amqp.connections").asScala.toList
    connections.head
  }
}