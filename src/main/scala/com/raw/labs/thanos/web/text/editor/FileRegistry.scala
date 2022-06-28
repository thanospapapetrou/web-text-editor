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

  protected def listFiles(): Seq[String]

  protected def getFile(name: String): Option[File]

  protected def createFile(file: File): Behavior[Command]

  protected def updateFile(file: File): Behavior[Command]

  protected def deleteFile(name: String): Behavior[Command]

  protected def registry(t: T): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetFiles(replyTo) =>
        println("Retrieving files")
        replyTo ! GetFilesResponse(listFiles())
        Behaviors.same
      case CreateFile(name, replyTo) =>
        println(s"Creating file $name")
        if (getFile(name).isEmpty) {
          val file = File(name, clock.millis, "")
          replyTo ! CreateFileResponse(Some(file), None)
          createFile(file)
        } else {
          replyTo ! CreateFileResponse(None, Some(FileRegistry.FILE_ALREADY_EXISTS))
          Behaviors.same
        }
      case GetFile(name, replyTo) =>
        println(s"Retrieving file $name")
        replyTo ! getFile(name)
        Behaviors.same
      case UpdateFile(name, request, replyTo) =>
        println(s"""Updating file $name with content "${request.content}" provided it has not been updated since ${Instant.ofEpochMilli(request.lastUpdated)}""")
        val existingFile = getFile(name)
        if (existingFile.isDefined) {
          if (existingFile.get.lastUpdated <= request.lastUpdated) {
            val newFile = File(name, if (request.content == existingFile.get.content) existingFile.get.lastUpdated else clock.millis, request.content)
            replyTo ! UpdateFileResponse(Some(newFile), None)
            updateFile(newFile)
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
        if (getFile(name).isDefined) {
          replyTo ! DeleteFileResponse(Some(name), None)
          deleteFile(name)
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