package com.raw.labs.thanos.web.text.editor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable

final case class File(name: String, lastUpdated: Long, content: String)
final case class Files(files: immutable.Seq[File])

object FileRegistry {
  sealed trait Command
  final case class GetFiles(replyTo: ActorRef[Files]) extends Command
  final case class NewFile(file: File, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetFile(name: String, replyTo: ActorRef[GetFileResponse]) extends Command
  final case class DeleteFile(name: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetFileResponse(maybeFile: Option[File])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(files: Set[File]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetFiles(replyTo) =>
        replyTo ! Files(files.toSeq)
        Behaviors.same
      case NewFile(file, replyTo) =>
        replyTo ! ActionPerformed(s"File ${file.name} created.")
        registry(files + file)
      case GetFile(name, replyTo) =>
        replyTo ! GetFileResponse(files.find(_.name == name))
        Behaviors.same
      case DeleteFile(name, replyTo) =>
        replyTo ! ActionPerformed(s"File $name deleted.")
        registry(files.filterNot(_.name == name))
    }
}
