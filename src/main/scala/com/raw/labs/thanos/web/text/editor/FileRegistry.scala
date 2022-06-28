package com.raw.labs.thanos.web.text.editor

import java.time.Clock

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.immutable.Seq

final case class File(name: String, lastUpdated: Long, content: String)

final case class GetFilesResponse(files: Seq[String])

final case class CreateFileResponse(file: Option[File], error: Option[String])

final case class UpdateFileResponse(file: Option[File], error: Option[String])

final case class DeleteFileResponse(file: Option[String], error: Option[String])

object FileRegistry {

  sealed trait Command

  final case class GetFiles(replyTo: ActorRef[GetFilesResponse]) extends Command

  final case class CreateFile(name: String, replyTo: ActorRef[CreateFileResponse]) extends Command

  final case class GetFile(name: String, replyTo: ActorRef[Option[File]]) extends Command

  final case class UpdateFile(name: String, content: String, replyTo: ActorRef[UpdateFileResponse]) extends Command

  final case class DeleteFile(name: String, replyTo: ActorRef[DeleteFileResponse]) extends Command

  implicit val clock: Clock = Clock.systemUTC()

  val FILE_ALREADY_EXISTS = "File already exists"
  val FILE_NOT_FOUND = "File not found"

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(files: Set[File]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetFiles(replyTo) =>
        println("Retrieving files")
        replyTo ! GetFilesResponse(files.toSeq.map(_.name).sorted)
        Behaviors.same
      case CreateFile(name, replyTo) =>
        println(s"Creating file $name")
        if (!files.exists(_.name == name)) {
          val file = File(name, clock.millis, "")
          replyTo ! CreateFileResponse(Some(file), None)
          registry(files + file)
        } else {
          replyTo ! CreateFileResponse(None, Some(FILE_ALREADY_EXISTS))
          Behaviors.same
        }
      case GetFile(name, replyTo) =>
        println(s"Retrieving file $name")
        replyTo ! files.find(_.name == name)
        Behaviors.same
      case UpdateFile(name, content, replyTo) =>
        println(s"""Updating file $name with content "$content"""")
        if (files.exists(_.name == name)) {
          val file = File(name, clock.millis, content)
          replyTo ! UpdateFileResponse(Some(file), None)
          registry(files.filterNot(_.name == name) + file)
          // TODO check timestamp
        } else {
          replyTo ! UpdateFileResponse(None, Some(FILE_NOT_FOUND))
          Behaviors.same
        }
      case DeleteFile(name, replyTo) =>
        println(s"Deleting file $name")
        if (files.exists(_.name == name)) {
          replyTo ! DeleteFileResponse(Some(name), None)
          registry(files.filterNot(_.name == name))
        } else {
          replyTo ! DeleteFileResponse(None, Some(FILE_NOT_FOUND))
          Behaviors.same
        }
    }
}
