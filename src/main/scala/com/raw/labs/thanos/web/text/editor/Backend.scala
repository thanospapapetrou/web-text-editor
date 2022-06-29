package com.raw.labs.thanos.web.text.editor

object Backend extends Enumeration {
  type Backend = Value

  val Memory, FileSystem, DB = Value

  def parse(backend: String): Backend = {
    Backend.values.find(_.toString == backend).orNull
  }
}
