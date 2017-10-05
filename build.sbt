name := "Rabbit Consumer"

version := "0.1"

scalaVersion := "2.12.2"

val scalazV = "7.1.11"
val scalazStreamV = "0.8.6"
val argonautV = "6.2"
val typesafeConfigV = "1.3.0"
val jodatimeV = "2.9.4"
val amqpClientV = "3.5.3"
val scalacheckV = "1.13.5"
val scalatestV = "3.0.4"

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

libraryDependencies ++= logging ++ scalacheck ++ scalatest ++ amqpClient ++ scalaz ++ argonaut ++ typesafeConfig

initialCommands in console :=
  """
    |import com.ppb.rabbitconsumer.{RabbitConsumer => R}
  """.stripMargin

