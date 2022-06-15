package com.raw.labs.thanos.web.text.editor

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.{Failure, Success}

object WebTextEditor {
  private val BINDING_FAILURE: String = "Failed to bind HTTP endpoint, terminating system"
  private val FILE_REGISTRY_ACTOR: String = "FileRegistryActor"
  private val HOST: String = "localhost"
  private val PORT: Int = 8080
  private val SERVER_ONLINE: String = "Server online at http://{}:{}/"
  private val WEB_TEXT_EDITOR: String = "WebTextEditor"

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext
    Http().newServerAt(HOST, PORT).bind(routes).onComplete {
      case Success(binding) =>
        system.log.info(SERVER_ONLINE, binding.localAddress.getHostString, binding.localAddress.getPort)
      case Failure(ex) =>
        system.log.error(BINDING_FAILURE, ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Behaviors.setup[Nothing] { context =>
      val actor = context.spawn(FileRegistry(), FILE_REGISTRY_ACTOR)
      context.watch(actor)
      startHttpServer(new WebTextEditorRoutes(actor)(context.system).fileRoutes)(context.system)
      Behaviors.empty
    }, WEB_TEXT_EDITOR)
  }
}
