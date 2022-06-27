package com.raw.labs.thanos.web.text.editor

import spray.json.DefaultJsonProtocol

object JsonFormats {

  import DefaultJsonProtocol._

  implicit val fileJsonFormat = jsonFormat3(File)
  implicit val filesJsonFormat = jsonFormat1(Files)
}
