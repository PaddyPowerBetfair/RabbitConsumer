name := "Rabbit Consumer"

version := "0.1"

scalaVersion := "2.12.6"

scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.rabbitmq" % "amqp-client" % "5.3.0",
  "io.circe" %% "circe-parser" % "0.9.3",
  "org.rogach" %% "scallop" % "3.1.2",
  "io.monix" %% "monix-nio" % "0.0.3",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test
)
