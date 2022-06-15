package com.raw.labs.thanos.web.text.editor

import com.raw.labs.thanos.web.text.editor.FileRegistry.ActionPerformed
import spray.json.DefaultJsonProtocol

object JsonFormats {

  import DefaultJsonProtocol._

  implicit val fileJsonFormat = jsonFormat3(File)
  implicit val filesJsonFormat = jsonFormat1(Files)
  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
