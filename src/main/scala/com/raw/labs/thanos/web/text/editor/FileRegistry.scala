package com.raw.labs.thanos.web.text.editor

import java.time.Clock

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.immutable.Seq

final case class File(name: String, lastUpdated: Long, content: String)

final case class Files(files: Seq[String])

object FileRegistry {

  sealed trait Command

  final case class GetFiles(replyTo: ActorRef[Files]) extends Command

  final case class CreateFile(name: String, replyTo: ActorRef[Option[File]]) extends Command

  final case class GetFile(name: String, replyTo: ActorRef[Option[File]]) extends Command

  final case class UpdateFile(name: String, content: String, replyTo: ActorRef[Option[File]]) extends Command

  final case class DeleteFile(name: String, replyTo: ActorRef[Option[String]]) extends Command

  implicit val clock: Clock = Clock.systemUTC()

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(files: Set[File]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetFiles(replyTo) =>
        println("Retrieving files")
        replyTo ! Files(files.toSeq.map(_.name).sorted)
        Behaviors.same
      case CreateFile(name, replyTo) =>
        println(s"Creating file $name")
        if (!files.exists(_.name == name)) {
          val file = File(name, clock.millis, "")
          replyTo ! Some(file)
          registry(files + file)
        } else {
          replyTo ! None
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
          replyTo ! Some(file)
          registry(files.filterNot(_.name == name) + file)
          // TODO check timestamp
        } else {
          replyTo ! None
          Behaviors.same
        }
      case DeleteFile(name, replyTo) =>
        println(s"Deleting file $name")
        if (files.exists(_.name == name)) {
          replyTo ! Some(name)
          registry(files.filterNot(_.name == name))
        } else {
          replyTo ! None
          Behaviors.same
        }
    }
}
