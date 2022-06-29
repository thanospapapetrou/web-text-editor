package com.raw.labs.thanos.web.text.editor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config

import scala.util.{Failure, Success}

object WebTextEditor {
  private val BACKEND: String = "web-text-editor.registry.backend"
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

  def getFileRegistry(config: Config): Behavior[Command] = {
    Backend.parse(config.getString(BACKEND)) match {
      case Backend.Memory => MemoryFileRegistry()
      case Backend.FileSystem => FileSystemFileRegistry(config)
      case Backend.DB => DbFileRegistry(config)
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Behaviors.setup[Nothing] { context =>
      val actor = context.spawn(getFileRegistry(context.system.settings.config), FILE_REGISTRY_ACTOR)
      context.watch(actor)
      startHttpServer(new WebTextEditorRoutes(actor)(context.system).routes)(context.system)
      Behaviors.empty
    }, WEB_TEXT_EDITOR)
  }
}
