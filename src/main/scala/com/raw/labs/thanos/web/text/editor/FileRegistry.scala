package com.raw.labs.thanos.web.text.editor

import java.time.{Clock, Instant}

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.immutable.Seq

final case class File(name: String, lastUpdated: Long, content: String)

final case class GetFilesResponse(files: Seq[String])

final case class CreateFileResponse(file: Option[File], error: Option[String])

final case class UpdateFileRequest(lastUpdated: Long, content: String)

final case class UpdateFileResponse(file: Option[File], error: Option[String])

final case class DeleteFileResponse(file: Option[String], error: Option[String])

sealed trait Command

final case class GetFiles(replyTo: ActorRef[GetFilesResponse]) extends Command

final case class CreateFile(name: String, replyTo: ActorRef[CreateFileResponse]) extends Command

final case class GetFile(name: String, replyTo: ActorRef[Option[File]]) extends Command

final case class UpdateFile(name: String, request: UpdateFileRequest, replyTo: ActorRef[UpdateFileResponse]) extends Command

final case class DeleteFile(name: String, replyTo: ActorRef[DeleteFileResponse]) extends Command

trait FileRegistry[T] {
  implicit val clock: Clock = Clock.systemUTC()

  protected def listFiles(context: T): Seq[String]

  protected def getFile(context: T, name: String): Option[File]

  protected def createFile(context: T, file: File): Behavior[Command]

  protected def updateFile(context: T, file: File): Behavior[Command]

  protected def deleteFile(context: T, name: String): Behavior[Command]

  protected def registry(context: T): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetFiles(replyTo) =>
        println("Retrieving files")
        replyTo ! GetFilesResponse(listFiles(context))
        Behaviors.same
      case CreateFile(name, replyTo) =>
        println(s"Creating file $name")
        if (getFile(context, name).isEmpty) {
          val file = File(name, clock.millis, "")
          replyTo ! CreateFileResponse(Some(file), None)
          createFile(context, file)
        } else {
          replyTo ! CreateFileResponse(None, Some(FileRegistry.FILE_ALREADY_EXISTS))
          Behaviors.same
        }
      case GetFile(name, replyTo) =>
        println(s"Retrieving file $name")
        replyTo ! getFile(context, name)
        Behaviors.same
      case UpdateFile(name, request, replyTo) =>
        println(s"""Updating file $name with content "${request.content}" provided it has not been updated since ${Instant.ofEpochMilli(request.lastUpdated)}""")
        val existingFile = getFile(context, name)
        if (existingFile.isDefined) {
          if (existingFile.get.lastUpdated <= request.lastUpdated) {
            val newFile = File(name, if (request.content == existingFile.get.content) existingFile.get.lastUpdated else clock.millis, request.content)
            replyTo ! UpdateFileResponse(Some(newFile), None)
            updateFile(context, newFile)
          } else {
            replyTo ! UpdateFileResponse(Some(existingFile.get), Some(FileRegistry.OPTIMISTIC_LOCK_FAILURE))
            Behaviors.same
          }
        } else {
          replyTo ! UpdateFileResponse(None, Some(FileRegistry.FILE_NOT_FOUND))
          Behaviors.same
        }
      case DeleteFile(name, replyTo) =>
        println(s"Deleting file $name")
        if (getFile(context, name).isDefined) {
          replyTo ! DeleteFileResponse(Some(name), None)
          deleteFile(context, name)
        } else {
          replyTo ! DeleteFileResponse(None, Some(FileRegistry.FILE_NOT_FOUND))
          Behaviors.same
        }
    }
}

object FileRegistry {
  val FILE_ALREADY_EXISTS = "File already exists"
  val FILE_NOT_FOUND = "File not found"
  val OPTIMISTIC_LOCK_FAILURE = "Optimistic lock failure"
}