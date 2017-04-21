package csw.services.config.server.http

import java.time.Instant

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.server.ServerWiring
import csw.services.config.server.commons.TestFileUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ConfigServiceRouteTest
    extends FunSuite
    with ScalatestRouteTest
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with HttpSupport {

  val serverWiring = new ServerWiring
  import serverWiring._
  import configServiceRoute.route

  private val testFileUtils = new TestFileUtils(settings)

  private val configValue1 = "axisName = tromboneAxis"
  private val configFile1  = ConfigData.fromString(configValue1)

  private val updatedConfigValue1 = "assemblyHCDCount = 3"
  private val updatedConfigFile1  = ConfigData.fromString(updatedConfigValue1)

  private val configValue2 = "name = NFIRAOS Trombone Assembly"
  private val configFile2  = ConfigData.fromString(configValue2)

  override protected def beforeAll(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def beforeEach(): Unit =
    svnRepo.initSvnRepo()

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  /**
   * test HTTP response codes
   */
  test("create - success status code") {
    // try to create by providing optional comment parameter
    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
      responseAs[ConfigId] shouldBe ConfigId(1)
    }

    // try to create by not providing optional comment parameter
    Post("/config/test1.conf?oversize=true", configFile2) ~> route ~> check {
      status shouldEqual StatusCodes.Created
      responseAs[ConfigId] shouldBe ConfigId(2)
    }

  }

  test("create - failure status codes") {

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Post("/config?oversize=true&comment=commit1", configFile1) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // try to create file which already exists
    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.Conflict
    }

  }

  test("update - success status code") {

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // try to update by providing optional comment parameter
    Put("/config/test.conf?comment=updated", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    // try to update by not providing optional comment parameter
    Put("/config/test.conf", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/config/test.conf") ~> route ~> check {
      responseAs[String] shouldEqual updatedConfigValue1
    }

  }

  test("update - failure status codes") {
    // path missing
    Post("/update") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // try to update file which does not exist
    Put("/config/test.conf?comment=updated", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // try to update a file which does not exist by not providing optional comment parameter
    Put("/config/test.conf", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("get - success status code") {
    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Get("/config/test.conf") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldEqual configValue1
    }

    Get("/config/test.conf?id=1") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldEqual configValue1
    }

  }

  test("get - failure status codes") {

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // try to fetch file which does not exists
    Get("/get?path=test1.conf") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.NotFound
    }

    // try to fetch version of file which does not exists
    Get("/config/test.conf?id=2") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.NotFound
    }

  }

  test("get by date - success status code") {
    val timeWhenRepoWasEmpty = Instant.now()
    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }
    val timeWhenFileWasCreated = Instant.now()

    Put("/config/test.conf?comment=updated", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get(s"/config/test.conf?date=$timeWhenFileWasCreated") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
    }

    Get(s"/config/test.conf?date=$timeWhenRepoWasEmpty") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("get by date - failure status codes") {
    val timeWhenRepoWasEmpty = Instant.now()

    Get(s"/config/test.conf?date=$timeWhenRepoWasEmpty") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  test("list - success status code") {
    // when list is empty
    Get("/list") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 0
    }

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // when list is not empty
    Get("/list") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 1
    }

  }

  test("history - success  status code") {

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Put("/config/test.conf?comment=commit2", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    val configFileHistoryIdCommentTuples = Set((ConfigId(1), "commit1"), (ConfigId(2), "commit2"))

    Get("/history/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileHistory]]
        .map(history => (history.id, history.comment))
        .toSet shouldEqual configFileHistoryIdCommentTuples
    }

    Get("/history/test.conf?maxResults=1") ~> route ~> check {
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
    Get("/history/test5.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    Get("/history/test5.conf&maxResults=5") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("getDefault - success status code") {

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Get("/config/test.conf?default=true") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[String] shouldEqual configValue1
    }

  }

  test("getDefault - failure status codes") {

    //  try to fetch default version of file which does not exists
    Get("/config/test1.conf?default=true") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("setDefault - success status code") {

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Put("/config/test.conf?comment=updated", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/default/test.conf?id=1") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/config/test.conf?default=true") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[String] shouldEqual configValue1
    }

  }

  test("setDefault - failure status codes") {

    // try to set default version of file which does not exist
    Put("/default/test.conf?id=1") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // try to set default version of file which exist bu corresponding id does not exist
    Put("/default/test.conf?id=2") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("resetDefault - success status code") {

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Put("/config/test.conf?comment=updated", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/default/test.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/default/test.conf?id=1") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/default/test.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  test("resetDefault - failure status codes") {

    //  try to reset default version of file which does not exists
    Put("/default/test.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("exists - success status code") {

    Post("/config/test.conf?oversize=true&comment=commit1", configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Head("/config/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  test("exists - failure status code") {

    Head("/config/test.conf?id=3") ~> route ~> check {
      status shouldEqual StatusCodes.NotFound
    }
    Head("/config/test_not_exists.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

}
