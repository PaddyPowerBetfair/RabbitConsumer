package com.ppb.rabbitconsumer

import com.typesafe.config.Config

object ConfigService {
  def readExchange(config: Config): String = config.getString("exchangeName")
  def readQueue(config: Config): String = config.getString("queue")
  def readRoutingKey(config: Config): String = config.getString("routingKey")
  def getFilename(config: Config): String =
    config.getString("fileName").replaceFirst("^~", System.getProperty("user.home"))
}
