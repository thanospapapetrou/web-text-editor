package com.raw.labs.thanos.web.text.editor

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.raw.labs.thanos.web.text.editor.FileRegistry._

import scala.concurrent.Future

object WebTextEditorRoutes {
  private val FILES: String = "files"
  private val INDEX: String = "static/index.html"
  private val STATIC: String = "static"
  private val TIMEOUT: String = "web-text-editor.routes.ask-timeout"
}

class WebTextEditorRoutes(fileRegistry: ActorRef[FileRegistry.Command])(implicit val system: ActorSystem[_]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration(WebTextEditorRoutes.TIMEOUT))

  def getFiles(): Future[GetFilesResponse] =
    fileRegistry.ask(GetFiles)

  def getFile(name: String): Future[Option[File]] =
    fileRegistry.ask(GetFile(name, _))

  def createFile(name: String): Future[CreateFileResponse] =
    fileRegistry.ask(CreateFile(name, _))

  def updateFile(name: String, request: UpdateFileRequest): Future[UpdateFileResponse] =
    fileRegistry.ask(UpdateFile(name, request, _))

  def deleteFile(name: String): Future[DeleteFileResponse] =
    fileRegistry.ask(DeleteFile(name, _))

  val routes: Route =
    concat(
      pathPrefix("") {
        pathEndOrSingleSlash {
          getFromResource(WebTextEditorRoutes.INDEX)
        } ~
          getFromResourceDirectory(WebTextEditorRoutes.STATIC)
      },
      pathPrefix(WebTextEditorRoutes.FILES) {
        concat(
          pathEnd {
            concat(
              get {
                complete(getFiles())
              }
            )
          },
          path(Segment) { name =>
            concat(
              post {
                onSuccess(createFile(name)) { response =>
                  complete(if (response.error.isEmpty) StatusCodes.Created else StatusCodes.Conflict, response)
                }
              },
              get {
                onSuccess(getFile(name)) { file =>
                  complete(if (file.isEmpty) StatusCodes.NotFound else StatusCodes.OK, file)
                }
              },
              put {
                entity(as[UpdateFileRequest]) { request =>
                  onSuccess(updateFile(name, request)) { response =>
                    complete(if (response.error.isEmpty) StatusCodes.OK else (if (response.error.get == FileRegistry.FILE_NOT_FOUND) StatusCodes.NotFound else StatusCodes.Conflict), response)
                  }
                }
              },
              delete {
                onSuccess(deleteFile(name)) { response =>
                  complete(if (response.error.isEmpty) StatusCodes.OK else StatusCodes.NotFound, response)
                }
              }
            )
          }
        )
      }
    )
}
