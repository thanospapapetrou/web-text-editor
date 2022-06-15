package com.raw.labs.thanos.web.text.editor

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.raw.labs.thanos.web.text.editor.UserRegistry._

import scala.concurrent.Future

object WebTextEditorRoutes {
  private val TIMEOUT: String = "web-text-editor.routes.ask-timeout"
}

class WebTextEditorRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration(WebTextEditorRoutes.TIMEOUT))

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)

  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))

  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))

  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        pathEnd {
          concat(
            get {
              complete(getUsers())
            },
            post {
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { name =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getUser(name)) { response =>
                  complete(response.maybeUser)
                }
              }
            },
            delete {
              onSuccess(deleteUser(name)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            })
        })
    }
}
