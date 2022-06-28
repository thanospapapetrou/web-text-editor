package com.raw.labs.thanos.web.text.editor

import akka.actor.typed.Behavior

object MemoryFileRegistry extends FileRegistry[Set[File]] {
  implicit val files: Set[File] = Set.empty

  def apply(): Behavior[Command] = registry(Set.empty)

  override protected def listFiles(): Seq[String] = files.toSeq.map(_.name).sorted

  override protected def getFile(name: String): Option[File] = files.find(_.name == name)

  override protected def createFile(file: File): Behavior[Command] = registry(files + file)

  override protected def updateFile(file: File): Behavior[Command] = registry(files.filterNot(_.name == file.name) + file)

  override protected def deleteFile(name: String): Behavior[Command] = registry(files.filterNot(_.name == name))
}
