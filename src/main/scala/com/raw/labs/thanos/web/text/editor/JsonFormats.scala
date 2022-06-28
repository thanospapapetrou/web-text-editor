package com.raw.labs.thanos.web.text.editor

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats {

  import DefaultJsonProtocol._


  implicit val fileJsonFormat: RootJsonFormat[File] = jsonFormat3(File)
  implicit val filesJsonFormat: RootJsonFormat[GetFilesResponse] = jsonFormat1(GetFilesResponse)
  implicit val createFileResponseJsonFormat: RootJsonFormat[CreateFileResponse] = jsonFormat2(CreateFileResponse)
  implicit val updateFileRequestJsonFormat: RootJsonFormat[UpdateFileRequest] = jsonFormat2(UpdateFileRequest)
  implicit val updateFileResponseJsonFormat: RootJsonFormat[UpdateFileResponse] = jsonFormat2(UpdateFileResponse)
  implicit val deleteFileResponseJsonFormat: RootJsonFormat[DeleteFileResponse] = jsonFormat2(DeleteFileResponse)
}
