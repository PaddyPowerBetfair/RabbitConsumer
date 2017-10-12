package com.ppb.rabbitconsumer
import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfter, FlatSpec, FunSuite}

import scala.collection.JavaConverters._
import scala.collection.immutable

class ConnectionServiceSpec extends FlatSpec    {

  "Test Connection Service is " should "have IP,PORT,USERNAME,PASSWORD" in  new ConnectionServiceFixture {

    config.foreach { cfg =>

      val connectionFactory = ConnectionService.connectionFactory(cfg)
    assert(connectionFactory.getHost == cfg.getString("ip"), " IP Address do not match")
    assert(connectionFactory.getPort == cfg.getInt("port"), " PORT do not match")
    assert(connectionFactory.getUsername == cfg.getString("user"), " USER do not match")
    assert(connectionFactory.getPassword == cfg.getString("password") , " PASSWORD do not match")
    assert(connectionFactory.isSSL == cfg.getBoolean("useSSL"), " SSL in connection factory and Config do not match!")
    }

  }

  "Test docker rabbit Connection Service " should "have docker-rabbit hostname,PORT,USERNAME,PASSWORD" in  new ConnectionServiceFixture {

    dockerConfig.foreach { cfg =>

      val connectionFactory = ConnectionService.connectionFactory(cfg)
      assert(connectionFactory.getHost == cfg.getString("ip"), " IP Address do not match")
      assert("my-rabbit-server" == cfg.getString("ip"), " IP Address do not match")
      assert(connectionFactory.getPort == cfg.getInt("port"), " PORT do not match")
      assert(connectionFactory.getUsername == cfg.getString("user"), " USER do not match")
      assert(connectionFactory.getPassword == cfg.getString("password") , " PASSWORD do not match")
      assert(connectionFactory.isSSL == cfg.getBoolean("useSSL"), " SSL in connection factory and Config do not match!")
    }

  }
}

trait ConnectionServiceFixture {
  val config = ConfigFactory.load("local").getConfigList("amqp.connections").asScala.toList
  val dockerConfig = ConfigFactory.load("docker-rabbit-server").getConfigList("amqp.connections").asScala.toList

}
