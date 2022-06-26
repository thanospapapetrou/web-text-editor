package com.raw.labs.thanos.web.text.editor

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class WebTextEditorRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val userRegistry = testKit.spawn(FileRegistry())
  lazy val routes = new WebTextEditorRoutes(userRegistry).routes

  "WebTextEditorRoutes" should {
    "return no files if no present (GET /files)" in {
      val request = HttpRequest(uri = "/files")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"files":[]}""")
      }
    }

    "be able to create files (POST /files)" in {
      val name = "foo"
      val request = Post(s"/files/$name")
      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"description":"File foo created."}""")
      }
    }

    "be able to remove files (DELETE /files)" in {
      val request = Delete(uri = "/files/foo")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"description":"File foo deleted."}""")
      }
    }
  }
}
