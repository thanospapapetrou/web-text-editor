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
  private val TIMEOUT: String = "web-text-editor.routes.ask-timeout"
}

class WebTextEditorRoutes(fileRegistry: ActorRef[FileRegistry.Command])(implicit val system: ActorSystem[_]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration(WebTextEditorRoutes.TIMEOUT))

  def getFiles(): Future[Files] =
    fileRegistry.ask(GetFiles)

  def getFile(name: String): Future[GetFileResponse] =
    fileRegistry.ask(GetFile(name, _))

  def createFile(file: File): Future[ActionPerformed] =
    fileRegistry.ask(NewFile(file, _))

  def deleteFile(name: String): Future[ActionPerformed] =
    fileRegistry.ask(DeleteFile(name, _))

  val fileRoutes: Route =
    pathPrefix(WebTextEditorRoutes.FILES) {
      concat(
        pathEnd {
          concat(
            get {
              complete(getFiles())
            },
            post {
              entity(as[File]) { file =>
                onSuccess(createFile(file)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { name =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getFile(name)) { response =>
                  complete(response.maybeFile)
                }
              }
            },
            delete {
              onSuccess(deleteFile(name)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            })
        })
    }
}
