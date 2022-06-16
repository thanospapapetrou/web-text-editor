package com.raw.labs.thanos.web.text.editor

import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.immutable.Seq

final case class File(name: String, lastUpdated: Long, content: String)

final case class Files(files: Seq[String])

object FileRegistry {

  sealed trait Command

  final case class GetFiles(replyTo: ActorRef[Files]) extends Command

  final case class CreateFile(name: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetFile(name: String, replyTo: ActorRef[GetFileResponse]) extends Command

  final case class UpdateFile(name: String, content: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class DeleteFile(name: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetFileResponse(maybeFile: Option[File])

  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(files: Set[File]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetFiles(replyTo) =>
        replyTo ! Files(files.toSeq.map(_.name).sorted)
        Behaviors.same
      case CreateFile(name, replyTo) =>
        replyTo ! ActionPerformed(s"File ${name} created.")
        // TODO use clock
        registry(files + File(name, Instant.now().toEpochMilli, ""))
      case GetFile(name, replyTo) =>
        replyTo ! GetFileResponse(files.find(_.name == name))
        Behaviors.same
      case UpdateFile(name, content, replyTo) =>
        println(s"Updating file $name with content $content")
        replyTo ! ActionPerformed(s"File $name updated.")
        // TODO use clock
        registry(files.filterNot(_.name == name) + File(name, Instant.now().toEpochMilli, content))
      case DeleteFile(name, replyTo) =>
        replyTo ! ActionPerformed(s"File $name deleted.")
        registry(files.filterNot(_.name == name))
    }
}
