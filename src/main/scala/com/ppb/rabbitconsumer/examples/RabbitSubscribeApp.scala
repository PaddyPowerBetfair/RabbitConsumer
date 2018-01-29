package com.ppb.rabbitconsumer.examples

import java.io.File

import com.ppb.rabbitconsumer._
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.util.Try

object RabbitSubscribeApp extends App {


  def getConfigsFromFile(configFile: File): Configurations =
    Configurations(configFile.getName, ConfigFactory.parseFile(configFile).getConfigList("amqp.connections").asScala.toList)


  // the callback function which will take a rabbit response and print result
  // result is either RabbitMessage or Exception
  val subscritpionFun: (Try[RabbitResponse]) => Unit = (response) =>{
    println(" \n receiving response \n--------------------------------------------------------")
       response.getOrElse(NoMoreMessages) match {
         case RabbitMessage(rabbitResponse, header) => {
              println("Header = "+header)
              rabbitResponse match {
                case RabbitPlainMessage(message) => println(message)
                case RabbitJsonMessage(json) => print(json.spaces2)
              }
         }
         case RabbitException(th)=> th.printStackTrace()
       }
}




  val subscribe:(File) => Unit =  getConfigsFromFile _ andThen RabbitConsumer.subscribeMessages(ConnectionService.subscribe , subscritpionFun)


  val exitUsage:(String) => Unit = (message ) => {
    println(s"${message}  \n\t valid command line arguments are <connection config file>\n")
    System.exit(0);
  }


  /////////  execution starts //////////////

  println( "\n\n\t Rabbit Consumer App")
  println( "\n\n\t Copyright (c)  \n")
  println( s"\t Version := ${System.getProperty("prog.version")} , revision := ${System.getProperty("prog.revision")}\n ")


  if (args.length == 0) {
    exitUsage("")
  }
  else {
    subscribe(new File(args(0)))
  }
 // println(" All done , exiting")

}
