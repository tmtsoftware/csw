package csw.services.config.server.http

import akka.http.scaladsl.model._
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

  val configServiceRoute = new ConfigServiceRoute(configManager, actorRuntime)
  import configServiceRoute.route

  private val testFileUtils = new TestFileUtils(settings)

  private val configValue1 = "axisName = tromboneAxis"
  val configFile1 =
    Multipart.FormData(Multipart.FormData.BodyPart.Strict("conf",
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, configValue1), Map("fileName" → "test.conf")))

  private val configValue2 = "assemblyHCDCount = 3"
  val updatedConfigFile1 =
    Multipart.FormData(Multipart.FormData.BodyPart.Strict("conf",
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, configValue2), Map("fileName" → "test.conf")))

  val createRequestParameterList = "path=test.conf&oversize=true&comment=commit1"

  override protected def beforeAll(): Unit =
    testFileUtils.deleteServerFiles()

  override protected def beforeEach(): Unit =
    svnAdmin.initSvnRepo()

  override protected def afterEach(): Unit =
    testFileUtils.deleteServerFiles()

  test("create") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[ConfigId] shouldBe ConfigId(1)
    }

  }

  test("update") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Post("/update?path=test.conf&comment=updated", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[ConfigId] shouldBe ConfigId(2)
    }

    Get("/get?path=test.conf") ~> route ~> check {
      responseAs[String] shouldEqual configValue2
    }

  }

  test("get") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/get?path=test.conf") ~> route ~> check {
      responseAs[String] shouldEqual configValue1
    }

    Get("/get?path=test.conf&id=1") ~> route ~> check {
      responseAs[String] shouldEqual configValue1
    }
  }

  test("list") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/list") ~> route ~> check {
      responseAs[List[ConfigFileInfo]].size shouldBe 1
    }

  }

  test("history") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Post("/update?path=test.conf&comment=commit2", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    val configFileHistoryIdCommentTuples = Set((ConfigId(1), "commit1"), (ConfigId(2), "commit2"))

    Get("/history?path=test.conf") ~> route ~> check {
      responseAs[List[ConfigFileHistory]]
        .map(history => (history.id, history.comment))
        .toSet shouldEqual configFileHistoryIdCommentTuples
    }
    Get("/history?path=test.conf&maxResults=1") ~> route ~> check {
      responseAs[List[ConfigFileHistory]].size shouldEqual 1
      responseAs[List[ConfigFileHistory]]
        .map(history => (history.id, history.comment)) shouldEqual List((ConfigId(2), "commit2"))
    }

  }

  test("getDefault") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/getDefault?path=test.conf") ~> route ~> check {
      responseAs[String] shouldEqual configValue1
    }

  }

  test("setDefault and resetDefault") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Post("/update?path=test.conf&comment=updated", updatedConfigFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/getDefault?path=test.conf") ~> route ~> check {
      responseAs[String] shouldEqual configValue2
    }

    Post("/setDefault?path=test.conf&id=1") ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/getDefault?path=test.conf") ~> route ~> check {
      responseAs[String] shouldEqual configValue1
    }

    Post("/resetDefault?path=test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/getDefault?path=test.conf") ~> route ~> check {
      responseAs[String] shouldEqual configValue2
    }
  }

//  TODO: FixMe
  /*
  test("exists") {

    Post("/create?" + createRequestParameterList, configFile1) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/exists?path=test.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/exists?path=test_not_exists.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }
 */

}
