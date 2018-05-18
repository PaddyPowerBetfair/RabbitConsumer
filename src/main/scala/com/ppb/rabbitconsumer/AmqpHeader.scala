package com.ppb.rabbitconsumer

case class AmqpHeader(key: String, value: String)

object AmqpHeader {

  private val Header = "(.*)=(.*)".r

  def toHeaders(headers: Seq[String]): Map[String, AnyRef] =
    (headers collect {
      case Header(key, value) => (key, value)
    }).toMap
}
