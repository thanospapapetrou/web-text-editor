package com.raw.labs.thanos.web.text.editor

import java.nio.charset.StandardCharsets

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.Config

object FileSystemFileRegistry extends FileRegistry[java.io.File] {
  private val BASE_DIR = "web-text-editor.registry.baseDir"

  def apply(config: Config): Behavior[Command] = registry(new java.io.File(config.getString(FileSystemFileRegistry.BASE_DIR)))

  override protected def listFiles(baseDir: java.io.File): Seq[String] = baseDir.listFiles.filter(_.isFile).map(_.getName).toSeq

  override protected def getFile(baseDir: java.io.File, name: String): Option[File] = {
    val file = new java.io.File(baseDir, name)
    if (file.exists) Some(File(file.getName, file.lastModified, java.nio.file.Files.readString(file.toPath, StandardCharsets.UTF_8))) else None
  }

  override protected def createFile(baseDir: java.io.File, file: File): Behavior[Command] = {
    new java.io.File(baseDir, file.name).createNewFile()
    Behaviors.same
  }

  override protected def updateFile(baseDir: java.io.File, file: File): Behavior[Command] = {
    java.nio.file.Files.write(new java.io.File(baseDir, file.name).toPath, file.content.getBytes(StandardCharsets.UTF_8))
    Behaviors.same
  }

  override protected def deleteFile(baseDir: java.io.File, name: String): Behavior[Command] = {
    new java.io.File(baseDir, name).delete()
    Behaviors.same
  }
}
