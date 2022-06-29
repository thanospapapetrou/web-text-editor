package com.raw.labs.thanos.web.text.editor

import akka.actor.typed.Behavior

object MemoryFileRegistry extends FileRegistry[Set[File]] {
  def apply(): Behavior[Command] = registry(Set.empty)

  override protected def listFiles(files: Set[File]): Seq[String] = files.toSeq.map(_.name).sorted

  override protected def getFile(files: Set[File], name: String): Option[File] = files.find(_.name == name)

  override protected def createFile(files: Set[File], file: File): Behavior[Command] = registry(files + file)

  override protected def updateFile(files: Set[File], file: File): Behavior[Command] = registry(files.filterNot(_.name == file.name) + file)

  override protected def deleteFile(files: Set[File], name: String): Behavior[Command] = registry(files.filterNot(_.name == name))
}
