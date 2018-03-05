package com.ppb.rabbitconsumer

import java.text.SimpleDateFormat
import java.util
import java.util.{Calendar, Date}
import java.util.concurrent.Executors

import com.ppb.rabbitconsumer.RabbitConsumerAlgebra.{OnReceive, RabbitJsonMessage, RabbitPlainMessage, RabbitResponse}
import org.scalatest.FlatSpec
import argonaut._
import Argonaut._
import com.rabbitmq.client.BasicProperties
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scalaz.concurrent.Future
import com.ppb.rabbitconsumer.RabbitConnection.ScalaHeaderProperties

class OnReceiveSpec extends FlatSpec    {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

  val onMessageReceive : OnReceive = (timestamp, queue, response, header) => {
    println(dateFormat.format(timestamp)+" Got Message from Queue " + queue)
    println(" \t Header " + header)
    println(" \t Message "+ response.getOrElse("ERRRRRRRR").toString )
  }

  def registerListener(queueName:String,  messageParser: MessageParser = PlainMessageParser): OnReceive => String = {

    implicit val context = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    lazy val definedConsumer:OnReceive => String = (onReceiveFun) => {
        println("inside definedConsumer")
        val tryMsg = Try( RabbitPlainMessage("MESSAGE") )
        val header:java.util.Map[String,AnyRef] = new util.HashMap[String, AnyRef]();
        header.put("key1",new String("value1"))
        header.put("key2",new String("value2"))
        header.put("key3",null)

      val properties: BasicProperties = MockitoSugar.mock[BasicProperties]
      when(properties.getHeaders).thenReturn(header)
      when(properties.getTimestamp).thenReturn(Calendar.getInstance().getTime())

      val propertiesNullHeader: BasicProperties = MockitoSugar.mock[BasicProperties]
      when(propertiesNullHeader.getHeaders).thenReturn(null)

      val f: Future[Unit] = Future {
        onReceiveFun(properties.getTimestamp ,  "QueueName", tryMsg, properties.headerAsScalaMap )
      }

      f.start.after(1000)

       "CONNECTED"
    }
    definedConsumer;
  }



  println("Testing the  OnReceiveSpec")
  //val connection = registerListener("queue").(onMessageReceive)
 // println( connection );

}
