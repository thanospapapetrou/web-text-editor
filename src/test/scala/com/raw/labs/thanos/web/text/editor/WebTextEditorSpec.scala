package com.raw.labs.thanos.web.text.editor

import java.time.Instant

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class WebTextEditorSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  lazy val testKit = ActorTestKit()

  implicit def typedSystem = testKit.system

  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.classicSystem

  val fileRegistry = testKit.spawn(WebTextEditor.getFileRegistry(testKit.system.classicSystem.settings.config))
  lazy val routes = new WebTextEditorRoutes(fileRegistry).routes

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "Web Text Editor" should {
    "return no file names if no files exist (GET /files)" in {
      // expect
      Get(uri = "/files") ~> routes ~> check {
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
      Get(uri = "/files") ~> routes ~> check {
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
      val pattern = s"""^\\{"file"\\:\\{"content"\\:""\\,"lastUpdated"\\:(\\d+)\\,"name"\\:"$name"\\}\\}$$""".r
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
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(s"""{"error":"${FileRegistry.FILE_ALREADY_EXISTS}"}""")
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
        contentType should ===(ContentTypes.NoContentType)
        entityAs[String] should ===("")
      }
    }

    "update file if it exists and hasn't been modified meanwhile (PUT /files/{file}" in {
      // given
      val name = "foo"
      val content = "bar"
      val pattern = s"""^\\{"file"\\:\\{"content"\\:"$content"\\,"lastUpdated"\\:(\\d+)\\,"name"\\:"$name"\\}\\}$$""".r
      val before = Instant.now.toEpochMilli
      Post(s"/files/$name") ~> routes
      // expect
      Put(s"/files/$name").withEntity(Marshal(UpdateFileRequest(Instant.now.toEpochMilli, content)).to[MessageEntity].futureValue) ~> routes ~> check {
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

    "not update file if it exists but has been modified meanwhile (PUT /files/{file}" in {
      // given
      val name = "foo"
      val content = "bar"
      val pattern = s"""^\\{"error"\\:"${FileRegistry.OPTIMISTIC_LOCK_FAILURE}"\\,"file"\\:\\{"content"\\:""\\,"lastUpdated"\\:(\\d+)\\,"name"\\:"$name"\\}\\}$$""".r
      val before = Instant.now.toEpochMilli
      Post(s"/files/$name") ~> routes
      // expect
      Put(s"/files/$name").withEntity(Marshal(UpdateFileRequest(before, content)).to[MessageEntity].futureValue) ~> routes ~> check {
        val after = Instant.now.toEpochMilli
        status should ===(StatusCodes.Conflict)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should fullyMatch regex pattern
        pattern.findAllIn(entityAs[String]).matchData.next.group(1).toLong should be >= before
        pattern.findAllIn(entityAs[String]).matchData.next.group(1).toLong should be <= after
      }
      // cleanup
      Delete(uri = s"/files/$name") ~> routes
    }

    "not update file if it doesn't exist (PUT /files/{file}" in {
      // given
      val name = "foo"
      val lastUpdated = Instant.now.toEpochMilli
      val content = "bar"
      // expect
      Put(s"/files/$name").withEntity(Marshal(UpdateFileRequest(lastUpdated, content)).to[MessageEntity].futureValue) ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(s"""{"error":"${FileRegistry.FILE_NOT_FOUND}"}""")
      }
    }

    "delete file if it exists (DELETE /files/{file})" in {
      val name = "foo"
      Post(s"/files/$name") ~> routes
      Delete(uri = s"/files/$name") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(s"""{"file":"$name"}""")
      }
    }

    "not delete file if it doesn't exist (DELETE /files/{file})" in {
      val name = "foo"
      Delete(uri = s"/files/$name") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===(s"""{"error":"${FileRegistry.FILE_NOT_FOUND}"}""")
      }
    }
  }
}
