package com.raw.labs.thanos.web.text.editor

import spray.json.DefaultJsonProtocol

object JsonFormats {

  import DefaultJsonProtocol._


  implicit val fileJsonFormat = jsonFormat3(File)
  implicit val filesJsonFormat = jsonFormat1(GetFilesResponse)
  implicit val createFileResponseJsonFormat = jsonFormat2(CreateFileResponse)
  implicit val updateFileRequestJsonFormat = jsonFormat2(UpdateFileRequest)
  implicit val updateFileResponseJsonFormat = jsonFormat2(UpdateFileResponse)
  implicit val deleteFileResponseJsonFormat = jsonFormat2(DeleteFileResponse)
}
