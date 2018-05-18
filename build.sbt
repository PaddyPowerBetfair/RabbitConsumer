import sbt._
import Keys._
import Tests._

name := "Rabbit Consumer"

version := "0.1"

scalaVersion := "2.12.3"

val scalazV = "7.1.11"
val scalazStreamV = "0.8.6"
val argonautV = "6.2"
val typesafeConfigV = "1.3.0"
val jodatimeV = "2.9.4"
val amqpClientV = "3.5.3"
val scalacheckV = "1.13.5"
val scalatestV = "3.0.4"
val mockitoV = "2.10.0"
val shapelessV = "2.3.3"

val typesafeConfig = Seq(
  "com.typesafe" % "config" % typesafeConfigV
)

val scalaz = Seq(
  "org.scalaz" %% "scalaz-core" % scalazV,
  "org.scalaz.stream" %% "scalaz-stream" % scalazStreamV
)

val argonaut = Seq(
  "io.argonaut" %% "argonaut" % argonautV
)

val scalacheck = Seq(
  "org.scalacheck" %% "scalacheck" % scalacheckV
)

val scalatest = Seq(
  "org.scalatest" %% "scalatest" % scalatestV 
)

val amqpClient = Seq(
  "com.rabbitmq" % "amqp-client" % amqpClientV
)

val logging = Seq (
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

val shapeless = Seq(
  "com.chuusai" %% "shapeless" % shapelessV
)

val mockito = Seq (
  "org.mockito" % "mockito-core" % mockitoV % "test"
)

lazy val IntegrationTest = config("it") extend Test

libraryDependencies ++= logging ++ scalacheck ++ scalatest ++ amqpClient ++ scalaz ++ argonaut ++ typesafeConfig ++ mockito ++ shapeless

/*
libraryDependencies += {
  val version = scalaBinaryVersion.value match {
    case "2.10" => "1.0.3"
    case _ ⇒ "1.1.0"
  }
  "com.lihaoyi" % "ammonite" % version % "test" cross CrossVersion.full
}

sourceGenerators in Test += Def.task {
  val file = (sourceManaged in Test).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main.main(args) }""")
  Seq(file)
}.taskValue
*/

lazy val root = (project in file("."))
.configs(IntegrationTest)
.settings(
   Defaults.itSettings
  )

initialCommands in console :=
  """
     |import com.ppb.rabbitconsumer.RabbitConsumer.{read => r}
     |import com.ppb.rabbitconsumer.RabbitPublisher.{publish => p}
     |import com.ppb.rabbitconsumer.RabbitConsumer.done
     |
     |""".stripMargin

