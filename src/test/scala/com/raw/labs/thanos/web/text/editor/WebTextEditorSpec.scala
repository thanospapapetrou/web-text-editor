package com.raw.labs.thanos.web.text.editor

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class WebTextEditorSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  val userRegistry = testKit.spawn(FileRegistry())
  lazy val routes = new WebTextEditorRoutes(userRegistry).routes

  "Web Text Editor" should {
    "return no file names if no files exist (GET /files)" in {
      // expect
      HttpRequest(uri = "/files") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"files":[]}""")
      }
    }

    "return file names if any files exist (GET /files)" in {
      // given
      val name = "foo"
      Post(s"/files/$name") ~> routes
      // expect
      HttpRequest(uri = "/files") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(s"""{"files":["$name"]}""")
      }
      // cleanup
      Delete(uri = s"/files/$name") ~> routes
    }

    "create file if it doesn't exist (POST /files)" in {
      // given
      val name = "foo"
      val pattern = s"""^\\{"content"\\:""\\,"lastUpdated"\\:(\\d+)\\,"name"\\:"$name"\\}$$""".r
      // expect
      val before = Instant.now.toEpochMilli
      Post(s"/files/$name") ~> routes ~> check {
        val after = Instant.now.toEpochMilli
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should fullyMatch regex pattern
        pattern.findAllIn(entityAs[String]).matchData.next.group(1).toLong should be >= before
        pattern.findAllIn(entityAs[String]).matchData.next.group(1).toLong should be <= after
      }
      // cleanup
      Delete(uri = s"/files/$name") ~> routes
    }

    "not create file if it already exists (POST /files)" in {
      // given
      val name = "foo"
      Post(s"/files/$name") ~> routes
      // expect
      Post(s"/files/$name") ~> routes ~> check {
        status should ===(StatusCodes.Conflict)
        entityAs[String] should ===("")
      }
      // cleanup
      Delete(uri = s"/files/$name") ~> routes
    }

    "return file if exists (GET /files/{file})" in {
      // given
      val name = "foo"
      val pattern = s"""^\\{"content"\\:""\\,"lastUpdated"\\:(\\d+)\\,"name"\\:"$name"\\}$$""".r
      val before = Instant.now.toEpochMilli
      Post(s"/files/$name") ~> routes
      // expect
      Get(s"/files/$name") ~> routes ~> check {
        val after = Instant.now.toEpochMilli
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should fullyMatch regex pattern
        pattern.findAllIn(entityAs[String]).matchData.next.group(1).toLong should be >= before
        pattern.findAllIn(entityAs[String]).matchData.next.group(1).toLong should be <= after
      }
      // cleanup
      Delete(uri = s"/files/$name") ~> routes
    }

    "not return file if it doesn't exist (GET /files/{file})" in {
      // given
      val name = "foo"
      // expect
      Get(s"/files/$name") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
        entityAs[String] should ===("")
      }
    }

    "delete file (DELETE /files/{file})" in {
      val name = "foo"
      Post(s"/files/$name") ~> routes
      Delete(uri = s"/files/$name") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"description":"File foo deleted."}""")
      }
    }
  }
}