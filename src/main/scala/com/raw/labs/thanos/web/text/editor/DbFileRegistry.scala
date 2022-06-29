package com.raw.labs.thanos.web.text.editor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.typesafe.config.Config
import doobie.implicits._
import doobie.util.transactor.Transactor

object DbFileRegistry extends FileRegistry[Transactor.Aux[IO, Unit]] {
  private val DRIVER = "web-text-editor.registry.jdbc.driver"
  private val URL = "web-text-editor.registry.jdbc.url"
  private val USERNAME = "web-text-editor.registry.jdbc.username"
  private val PASSWORD = "web-text-editor.registry.jdbc.password"

  def apply(config: Config): Behavior[Command] = registry(Transactor.fromDriverManager(
    config.getString(DRIVER),
    config.getString(URL),
    config.getString(USERNAME),
    config.getString(PASSWORD)
  ))

  override protected def listFiles(transactor: Transactor.Aux[IO, Unit]): Seq[String] = sql"""
      SELECT name
      FROM file_registry
      """.query[String].to[Seq].transact(transactor).unsafeRunSync()

  override protected def getFile(transactor: Transactor.Aux[IO, Unit], name: String): Option[File] = sql"""
      SELECT name, lastUpdated, content
      FROM file_registry WHERE name = $name
      """.query[File].option.transact(transactor).unsafeRunSync

  override protected def createFile(transactor: Transactor.Aux[IO, Unit], file: File): Behavior[Command] = {
    sql"""
        INSERT INTO file_registry (name, lastUpdated, content)
        VALUES (${file.name}, ${file.lastUpdated}, ${file.content})
        """.update.run.transact(transactor).unsafeRunSync
    Behaviors.same
  }

  override protected def updateFile(transactor: Transactor.Aux[IO, Unit], file: File): Behavior[Command] = Behaviors.same

  override protected def deleteFile(transactor: Transactor.Aux[IO, Unit], name: String): Behavior[Command] = Behaviors.same
}
