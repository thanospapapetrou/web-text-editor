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

  def getFiles(): Future[Files] =
    fileRegistry.ask(GetFiles)

  def getFile(name: String): Future[GetFileResponse] =
    fileRegistry.ask(GetFile(name, _))

  def createFile(name: String): Future[ActionPerformed] =
    fileRegistry.ask(CreateFile(name, _))

  def updateFile(name: String, content: String): Future[ActionPerformed] =
    fileRegistry.ask(UpdateFile(name, content, _))

  def deleteFile(name: String): Future[ActionPerformed] =
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
                onSuccess(createFile(name)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              },
              get {
                rejectEmptyResponse {
                  onSuccess(getFile(name)) { response =>
                    complete(response.maybeFile)
                  }
                }
              },
              put {
                entity(as[String]) { content =>
                  onSuccess(updateFile(name, content)) { response =>
                    complete(response)
                  }
                }
              },
              delete {
                onSuccess(deleteFile(name)) { performed =>
                  complete((StatusCodes.OK, performed))
                }
              }
            )
          }
        )
      }
    )
}
