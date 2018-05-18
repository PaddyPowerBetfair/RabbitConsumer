package com.ppb.rabbitconsumer

trait JsonWriter[T] {
  def toJson(t: T): String
}

object JsonWriterSyntax {
  implicit class JsonWriterOps[A](value: A) {
    def toJson(implicit jsonWriter: JsonWriter[A]): String = {
      jsonWriter.toJson(value)
    }
  }
}

