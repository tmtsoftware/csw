package csw.config.server.http

import java.time.Instant

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.commons.http.ErrorResponse
import csw.config.api.models.{ConfigData, ConfigFileInfo, ConfigFileRevision, ConfigId, _}
import csw.config.server.ServerWiring
import csw.config.server.commons.TestFileUtils
import csw.config.server.mocks.MockedAuthentication
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

// DEOPSCSW-80: HTTP based access for configuration file
// DEOPSCSW-576: Auth token for Configuration service
class ConfigServiceRouteTest
    extends FunSuite
    with ScalatestRouteTest
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with HttpSupport
    with MockitoSugar
    with MockedAuthentication {

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  val serverWiring: ServerWiring = ServerWiring.make(securityDirectives)
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
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // try to create by not providing optional comment parameter
    Post("/config/test1.conf?annex=true", configFile2).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

  }

  test("create - failure status codes") {

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Post("/config?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // try to create file which already exists
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.Conflict
    }

  }

  test("update - success status code") {

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // try to update by providing optional comment parameter
    Put("/config/test.conf?comment=updated", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    // try to update by not providing optional comment parameter
    Put("/config/test.conf", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/config/test.conf") ~> route ~> check {
      responseAs[String] shouldEqual updatedConfigValue1
    }

  }

  test("update - failure status codes") {
    // path missing
    Post("/update").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // try to update file which does not exist
    Put("/config/test.conf?comment=updated", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // try to update a file which does not exist by not providing optional comment parameter
    Put("/config/test.conf", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("get - success status code") {
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
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

    //consumes 2 revisions, one for actual file one for active file
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // try to fetch file which does not exists
    Get("/get?path=test1.conf") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.NotFound
    }

    // try to fetch version of file which does not exists
    Get("/config/test.conf?id=3") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.NotFound
    }

  }

  test("get by date - success status code") {
    val timeWhenRepoWasEmpty = Instant.now()
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }
    val timeWhenFileWasCreated = Instant.now()

    Put("/config/test.conf?comment=updated", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
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

  test("get latest - success status code") {
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Put("/config/test.conf?comment=updated", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/active-version/test.conf?id=1").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/config/test.conf") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldEqual updatedConfigValue1
    }
  }

  test("get latest - failure status code") {
    // try to fetch file which does not exists
    Get("/get?path=test1.conf") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  test("list - success status code") {
    // when list is empty
    Get("/list") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 0
    }

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // when list is not empty
    Get("/list") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 1
    }
  }

  test("list by pattern - success code") {
    Get("/list?pattern=a/b") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 0
    }

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Get("/list?pattern=.*.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 1
    }
  }

  test("list by pattern - rejection") {
    Get("/list?pattern=?i)") ~> route ~> check {
      status shouldEqual StatusCodes.BadRequest
      responseAs[ErrorResponse].error.code shouldBe StatusCodes.BadRequest.intValue
      responseAs[ErrorResponse].error.message should not be empty
    }
  }

  test("list by file type - success code") {
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Get("/list?type=Annex") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 1
    }

    Get("/list?type=Normal") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 0
    }
  }

  test("list by file type - rejection") {
    Get("/list?type=invalidtype") ~> route ~> check {
      status shouldEqual StatusCodes.BadRequest
      responseAs[ErrorResponse].error.code shouldBe StatusCodes.BadRequest.intValue
      responseAs[ErrorResponse].error.message should not be empty
    }
  }

  test("list by file type and pattern - success code") {
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Get("/list?type=Annex&pattern=.*.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 1
    }
  }

  test("history - success  status code") {

    //consumes 2 revisions, one for actual file one for active file
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    val timeWhenFileWasCreated = Instant.now()

    Put("/config/test.conf?comment=commit2", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    val timeWhenFileWasUpdated = Instant.now()

    val configFileHistoryIdCommentTuples = Set((ConfigId(1), "commit1"), (ConfigId(3), "commit2"))

    Get("/history/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.comment))
        .toSet shouldEqual configFileHistoryIdCommentTuples
    }

    Get("/history/test.conf?maxResults=1") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.comment)) shouldEqual List((ConfigId(3), "commit2"))
    }

    Get(s"/history/test.conf?maxResults=1&from=$timeWhenFileWasCreated&to=$timeWhenFileWasUpdated") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.comment)) shouldEqual List((ConfigId(3), "commit2"))
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

    Get("/history/invalid=/chars/in/path.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.BadRequest
    }

  }

  test("getActive - success status code") {

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Get("/active-config/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[String] shouldEqual configValue1
    }
  }

  test("getActive - failure status codes") {

    //  try to fetch active version of file which does not exists
    Get("/active-config/test1.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("getActive by date - success status code") {

    val timeWhenRepoWasEmpty = Instant.now()
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    val timeWhenFileWasCreated = Instant.now()

    Put("/config/test.conf?comment=updated", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/active-version/test.conf?id=3&comment=commit1").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    val timeWhenFileWasUpdated = Instant.now()

    Put("/config/test.conf?comment=updated", configFile2).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get(s"/active-config/test.conf?date=$timeWhenFileWasCreated") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[String] shouldEqual configValue1
    }

    Get(s"/active-config/test.conf?date=$timeWhenFileWasUpdated") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[String] shouldEqual updatedConfigValue1
    }

    Get(s"/active-config/test.conf?date=$timeWhenRepoWasEmpty") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("getActive by date - failure status codes") {

    //  try to fetch active version of file which does not exists
    Get("/active-config/test1.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    Get(s"/active-config?date=${Instant.now}") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  test("setActive - success status code") {

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Put("/config/test.conf?comment=updated", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/active-version/test.conf?id=1&comment=commit1").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Get("/active-config/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[String] shouldEqual configValue1
    }
  }

  test("setActive - failure status codes") {

    // try to set active version of file which does not exist
    Put("/active-version/test.conf?id=1&comment=commit1").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    //consumes 2 revisions, one for actual file one for active file
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // try to set active version of file which exist but corresponding id does not exist
    Put("/active-version/test.conf?id=3&comment=commit1").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("resetActive - success status code") {

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Put("/config/test.conf?comment=updated", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/active-version/test.conf").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/active-version/test.conf?id=1").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/active-version/test.conf").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  test("resetActive - failure status codes") {

    //  try to reset active version of file which does not exists
    Put("/active-version/test.conf").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("history-active - success  status code") {

    //consumes 2 revisions, one for actual file one for active file
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    val timeWhenFileWasCreated = Instant.now()

    Put("/config/test.conf?comment=commit2", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    Put("/active-version/test.conf?id=3&comment=commit1").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    val timeWhenFileWasUpdated = Instant.now()

    val configFileHistoryIdCommentTuples =
      Set((ConfigId(1), "initializing active file with the first version"), (ConfigId(3), "commit1"))

    Get("/history-active/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.comment))
        .toSet shouldEqual configFileHistoryIdCommentTuples
    }

    Get("/history-active/test.conf?maxResults=1") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.comment)) shouldEqual List((ConfigId(3), "commit1"))
    }

    Get(s"/history-active/test.conf?maxResults=1&from=$timeWhenFileWasCreated&to=$timeWhenFileWasUpdated") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.comment)) shouldEqual List((ConfigId(3), "commit1"))
    }

  }

  test("history-active - failure  status codes") {

    // query parameter missing
    Get("/history-active") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    // try to fetch history of a file which does not exists
    Get("/history-active/test5.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    Get("/history-active/invalid=/chars/in/path.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.BadRequest
    }

  }

  test("exists - success status code") {

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
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

  test("getMetadata - success status code") {
    Get("/metadata") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[ConfigMetadata].repoPath should not be empty
      responseAs[ConfigMetadata].annexPath should not be empty
      responseAs[ConfigMetadata].annexMinFileSize should not be empty
      responseAs[ConfigMetadata].maxConfigFileSize should not be empty
    }
  }

  // DEOPSCSW-576: Auth token for Configuration service
  /** Auth Based routes **/
  /* ================ Unauthorized code ================*/
  test("create - Unauthorized code") {
    Post("/config/test.conf?comment=create") ~> route ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  test("update - Unauthorized code") {
    Put("/config/test.conf?comment=update") ~> route ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  test("delete - Unauthorized code") {
    Delete("/config/test.conf?comment=deleting") ~> route ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  test("set active-version - Unauthorized code") {
    Put("/active-version/test.conf?id=1&comment=active") ~> route ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  /* ================ Forbidden code ================*/
  test("create - Forbidden code") {
    Post("/config/test.conf?comment=create").addHeader(roleMissingTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  test("update - Forbidden code") {
    Put("/config/test.conf?comment=update").addHeader(roleMissingTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  test("set active-version - Forbidden code") {
    Put("/active-version/test.conf?id=1&comment=active").addHeader(roleMissingTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  test("delete - Forbidden code") {
    Delete("/config/test.conf?comment=deleting").addHeader(roleMissingTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }
}
