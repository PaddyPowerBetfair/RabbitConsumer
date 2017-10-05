name := "Rabbit Consumer"

version := "0.1"

scalaVersion := "2.12.2"

val typesafeConfigV = "1.3.0"

val typesafeConfig = Seq(
  "com.typesafe" % "config" % typesafeConfigV
)

val scalacheck = Seq(
    "org.scalacheck" %% "scalacheck" % "1.12.2"
)

val scalatest = Seq(
    "org.scalatest" %% "scalatest" % "2.2.4"
)

val logging = Seq (
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

libraryDependencies ++= logging 

initialCommands in console :=
  """
    |import com.ppb.rabbitconsumer.{RabbitConsumer => R}
  """.stripMargin

