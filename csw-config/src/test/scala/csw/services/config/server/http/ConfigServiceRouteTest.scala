package csw.services.config.server.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.services.config.api.commons.TestFileUtils
import csw.services.config.api.models.{ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.server.ServerWiring
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ConfigServiceRouteTest extends FunSuite
    with ScalatestRouteTest
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with JsonSupport {

  val serverWiring = new ServerWiring
  import serverWiring._
  import configServiceRoute.route

  private val testFileUtils = new TestFileUtils(settings)

  private val configValue1 = "axisName = tromboneAxis"
  val configFile1 =
    Multipart.FormData(Multipart.FormData.BodyPart.Strict("conf",
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, configValue1), Map("fileName" → "test.conf")))

  private val updatedConfigValue1 = "assemblyHCDCount = 3"
  val updatedConfigFile1 =
    Multipart.FormData(Multipart.FormData.BodyPart.Strict("conf",
      HttpEntity(ContentTypes.`text/plain(UTF-8)`, updatedConfigValue1), Map("fileName" → "test.conf")))

  private val configValue2 = "name = NFIRAOS Trombone Assembly"
  val configFile2 =
    Multipart.FormData(Multipart.FormData.BodyPart.Strict("conf",
      HttpEntity(ContentTypes.`text/plain(UTF-8)`, configValue2), Map("fileName" → "test1.conf")))

  val createRequestParameterList = "path=test.conf&oversize=true&comment=commit1"

  override protected def beforeAll(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def beforeEach(): Unit =
    svnAdmin.initSvnRepo()

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  /**
    * test HTTP response codes
    */

  test("create - success status code") {
    // try to create by providing optional comment parameter
    Post("/create?path=test.conf&oversize=true&comment=commit1", configFile1) ~> route ~> check {
      // actual
      status shouldEqual StatusCodes.OK
      // expected
      // status shouldEqual StatusCodes.Created
      responseAs[ConfigId] shouldBe ConfigId(1)
    }

    // try to create by not providing optional comment parameter
    Post("/create?path=test1.conf&oversize=true", configFile2) ~> route ~> check {
      // actual
      status shouldEqual StatusCodes.OK
      // expected
      // status shouldEqual StatusCodes.Created
      responseAs[ConfigId] shouldBe ConfigId(2)
    }

  }

  test("create - failure status codes") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      // actual
      status shouldEqual StatusCodes.OK
      // expected
      // status shouldEqual StatusCodes.Created
    }

    // invalid query parameter's
    Post("/create?path=test1.conf&oversize=true&comment=commit") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.UnsupportedMediaType
    }

    Post("/create?oversize=true&comment=commit1", configFile1) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    //  method not allowed
    Put("/create?path=test.conf&oversize=true&comment=commit1", configFile1) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.MethodNotAllowed
    }

    // try to create file which already exists
    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      // expected
      // status shouldEqual StatusCodes.Conflict
      // status shouldEqual StatusCodes.UnprocessableEntity

      // actual
      status shouldEqual StatusCodes.BadRequest
    }

  }

  test("update - success status code") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    // try to update by providing optional comment parameter
    Post("/update?path=test.conf&comment=updated", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    // try to update by not providing optional comment parameter
    Post("/update?path=test.conf", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/get?path=test.conf") ~> route ~> check {
      responseAs[String] shouldEqual updatedConfigValue1
    }

  }

  test("update - failure status codes") {
    // query parameter missing
    Post("/update") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // method not allowed
    Put("/update?path=test.conf&comment=updated") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.MethodNotAllowed
    }

    // try to update file which does not exist
    Post("/update?path=test.conf&comment=updated", configFile1) ~> route ~> check {
      // expected
      // status shouldEqual StatusCodes.NotFound

      //  actual
      status shouldEqual StatusCodes.BadRequest
    }

    // try to update a file which does not exist by not providing optional comment parameter
    Post("/update?path=test.conf", configFile1) ~> route ~> check {
      // expected
      // status shouldEqual StatusCodes.NotFound

      //  actual
      status shouldEqual StatusCodes.BadRequest
    }

  }

  test("get - success status code") {
    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/get?path=test.conf") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldEqual configValue1
    }

    Get("/get?path=test.conf&id=1") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldEqual configValue1
    }

  }

  test("get - failure status codes") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    // try to fetch file which does not exists
    Get("/get?path=test1.conf") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.NotFound
    }

    // try to fetch version of file which does not exists
    Get("/get?path=test.conf&id=2") ~> Route.seal(route) ~> check {
    // is it correct ?
      status shouldBe StatusCodes.InternalServerError
    }

  }

  test("list - success status code") {
    // when list is empty
    Get("/list") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 0
    }

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }
    // when list is not empty
    Get("/list") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 1
    }

  }

  test("history - success  status code") {

    Post("/create?path=test.conf&oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Post("/update?path=test.conf&comment=commit2", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    val configFileHistoryIdCommentTuples = Set((ConfigId(1), "commit1"), (ConfigId(2), "commit2"))

    Get("/history?path=test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileHistory]]
        .map(history => (history.id, history.comment))
        .toSet shouldEqual configFileHistoryIdCommentTuples
    }

    Get("/history?path=test.conf&maxResults=1") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileHistory]]
        .map(history => (history.id, history.comment)) shouldEqual List((ConfigId(2), "commit2"))
    }

  }

  test("history - failure  status codes") {
    // query parameter missing
    Get("/history") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // try to fetch history of a file which does not exists
    Get("/history?path=test5.conf") ~> Route.seal(route) ~> check {
      // expected
      // status shouldEqual StatusCodes.NotFound

      //  actual
      status shouldEqual StatusCodes.OK
    }

    Get("/history?path=test5.conf&maxResults=5") ~> Route.seal(route) ~> check {
      // expected
      // status shouldEqual StatusCodes.NotFound

      //  actual
      status shouldEqual StatusCodes.OK
    }

  }

  test("getDefault - success status code") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/getDefault?path=test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[String] shouldEqual configValue1
    }

  }

  test("getDefault - failure status codes") {

    // query parameter missing
    Get("/getDefault") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }
    //  try to fetch default version of file which does not exists
    Get("/getDefault?path=test1.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("setDefault - success status code") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Post("/update?path=test.conf&comment=updated", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Post("/setDefault?path=test.conf&id=1") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/getDefault?path=test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[String] shouldEqual configValue1
    }

  }

  test("setDefault - failure status codes") {

    // query parameter missing
    Post("/setDefault") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // method not allowed
    Put("/setDefault?path=test.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.MethodNotAllowed
    }

    // try to set default version of file which does not exist
    Post("/setDefault?path=test.conf&id=1") ~> Route.seal(route) ~> check {
      //  actual
      status shouldEqual StatusCodes.OK

      //  expected
      //  status shouldEqual StatusCodes.NotFound
    }

    // try to set default version of file which does not exist
    Post("/setDefault?path=test.conf") ~> Route.seal(route) ~> check {
      //  actual
      status shouldEqual StatusCodes.BadRequest
      //  expected
      //  status shouldEqual StatusCodes.NotFound
    }

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    // try to set default version of file which exist bu corresponding id does not exist
    Post("/setDefault?path=test.conf&id=2") ~> Route.seal(route) ~> check {
      //  actual
      status shouldEqual StatusCodes.BadRequest

      //  expected
      //  status shouldEqual StatusCodes.NotFound
    }

  }

  test("resetDefault - success status code") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Post("/update?path=test.conf&comment=updated", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Post("/resetDefault?path=test.conf") ~> Route.seal(route) ~> check {
      //  actual
      status shouldEqual StatusCodes.BadRequest

      //  expected
      //  status shouldEqual StatusCodes.OK
    }

    Post("/setDefault?path=test.conf&id=1") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Post("/resetDefault?path=test.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  test("resetDefault - failure status codes") {

    // query parameter missing
    Post("/resetDefault") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // method not allowed
    Put("/resetDefault?path=test.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.MethodNotAllowed
    }

    //  try to reset default version of file which does not exists
    Post("/resetDefault?path=test.conf") ~> Route.seal(route) ~> check {
      //  actual
      status shouldEqual StatusCodes.BadRequest

      //  expected
      //  status shouldEqual StatusCodes.NotFound
    }

  }

  test("exists - success status code") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/exists?path=test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

  }

  test("exists - failure status code") {

    Get("/exists?path=test_not_exists.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

}
