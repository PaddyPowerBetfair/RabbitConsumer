package com.ppb.rabbitconsumer.examples

import java.io.File
import java.text.SimpleDateFormat

import com.ppb.rabbitconsumer.RabbitConsumerAlgebra._
import com.ppb.rabbitconsumer.{ConnectionService, _}
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scalaz.stream.{Process, Process0, text, _}

object RabbitExampleApp  extends App {




  def getConfigsFromFile(configFile: File): Configurations =
    Configurations(configFile.getName, ConfigFactory.parseFile(configFile).getConfigList("amqp.connections").asScala.toList)


  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    val onReceiveFun: (Config) => OnReceive = (config) => {

      lazy val outStream = Option(ConfigService.getFilename(config)).getOrElse(None) match {
        case file:String => {io.fileChunkW(file, 4096, true)}
        case None => {io.stdOutBytes}
      }

      val myReceiveFun:OnReceive = (timestamp, queueName, response, header)  => {
        println(" Called on onReceive callback " + queueName + " response " + response)

        val headerStream = Option(header).getOrElse(None) match {
          case  headerVal => Process("Header={"+headerVal.toString+"}")
          case None=> Process.emit("")
        }


        val responseStream = response.getOrElse(NoMoreMessages) match {
          case RabbitPlainMessage(message) => Process.emit( message)
          case RabbitJsonMessage(json) => Process.emit(json.spaces2)
          case RabbitException(th) => Process.emit(th.toString)
          case _ => Process.halt
        }
        val partialProc:Process0[String] =   Process(dateFormat.format(timestamp))  ++ headerStream ++ responseStream ++ Process("\n")

        (partialProc.intersperse("\t").toSource pipe text.utf8Encode to outStream).run.run
      }

      myReceiveFun
    }


    val doSubscribe:(File) => Unit = getConfigsFromFile _ andThen RabbitConsumer.subscribeMessages(ConnectionService.subscribe(_) , onReceiveFun )

  val exitUsage:(String) => Unit = (message ) => {
    println(s"${message}  \n\t valid command line arguments are <connection config file>\n")
    System.exit(0);
  }





  println("\n\n\t Copyright (c)  \n")
  println(s"\t Version := ${System.getProperty("prog.version")} , revision := ${System.getProperty("prog.revision")}\n ")
  println(" ============> args "+ args(0))
  if (args.length == 0) exitUsage("")

  if (args.length == 1) {
    println("\n\n\t Rabbit Consumer App")
    RabbitConsumer.readFromFile
  } else {
    println("\n\n\t Rabbit Subscriber App")
    doSubscribe(new File(args(0)))
  }

}
