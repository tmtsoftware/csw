package csw.config.server.http

import java.time.Instant

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.commons.http.ErrorResponse
import csw.config.api.ConfigData
import csw.config.api.commons.Constants
import csw.config.models.{ConfigFileInfo, ConfigFileRevision, ConfigId, ConfigMetadata}
import csw.config.server.ServerWiring
import csw.config.server.commons.TestFileUtils
import csw.config.server.mocks.MockedAuthentication
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.collection.mutable.ArrayBuffer
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-80: HTTP based access for configuration file
// DEOPSCSW-576: Auth token for Configuration service
// DEOPSCSW-69: Use authorization token to get identity of user creating/updating a configuration file
// DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
// DEOPSCSW-626: Get route of config server with path for empty config file
class ConfigServiceRouteTest
    extends AnyFunSuite
    with ScalatestRouteTest
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with HttpParameter
    with MockitoSugar
    with MockedAuthentication {

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  val serverWiring: ServerWiring = ServerWiring.make(securityDirectives)
  import serverWiring._
  import configServiceRoute.route
  import csw.config.models.codecs.ConfigCodecs._
  import csw.commons.http.codecs.ErrorCodecs._

  private val testFileUtils = new TestFileUtils(settings)

  private val configValue1 = "axisName = tromboneAxis"
  private val configFile1  = ConfigData.fromString(configValue1)

  private val updatedConfigValue1 = "assemblyHCDCount = 3"
  private val updatedConfigFile1  = ConfigData.fromString(updatedConfigValue1)

  private val configValue2 = "name = NFIRAOS Trombone Assembly"
  private val configFile2  = ConfigData.fromString(configValue2)

  private val emptyString     = ""
  private val emptyConfigFile = ConfigData.fromString(emptyString)

  override protected def beforeAll(): Unit = testFileUtils.deleteServerFiles()

  override protected def beforeEach(): Unit = svnRepo.initSvnRepo()

  override protected def afterEach(): Unit = testFileUtils.deleteServerFiles()

  /**
   * test HTTP response codes
   */
  test("create - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    // try to create by providing optional comment parameter
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // try to create by not providing optional comment parameter
    Post("/config/test1.conf?annex=true", configFile2).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

  }

  test("create - failure status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

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

  test("update - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

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

  test("update - failure status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
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

  test("get - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
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

  test("get - failure status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

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

  //DEOPSCSW-626: Get route of config server with path for empty config file
  test("get - success status code for empty file | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Post("/config/test.conf?annex=true&comment=commit1", emptyConfigFile).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }
    Get("/config/test.conf") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldEqual Constants.EmptySourceContent
    }

    Get("/config/test.conf?id=1") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.OK
      responseAs[String] shouldEqual Constants.EmptySourceContent
    }
  }

  test("get by date - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
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

  test("get by date - failure status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    val timeWhenRepoWasEmpty = Instant.now()

    Get(s"/config/test.conf?date=$timeWhenRepoWasEmpty") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  test("get latest - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
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

  test("get latest - failure status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    // try to fetch file which does not exists
    Get("/get?path=test1.conf") ~> Route.seal(route) ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }

  test("list - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
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

  // DEOPSCSW-577: Ability to view detailed change log in SVN
  test("list - with correct author | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-577, DEOPSCSW-69") {
    Get("/list") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 0
    }

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Get("/list") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      val fileList = responseAs[List[ConfigFileInfo]]
      fileList.size shouldBe 1
      fileList.map(_.author) shouldBe List(preferredUserName)
    }
  }

  test("list by pattern - success code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
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

  test("list by pattern - rejection | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Get("/list?pattern=?i)") ~> route ~> check {
      status shouldEqual StatusCodes.BadRequest
      responseAs[ErrorResponse].error._type shouldBe None
      responseAs[ErrorResponse].error.message should not be empty
    }
  }

  test("list by file type - success code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
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

  test("list by file type - rejection | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Get("/list?type=invalidtype") ~> route ~> check {
      status shouldEqual StatusCodes.BadRequest
      responseAs[ErrorResponse].error._type shouldBe None
      responseAs[ErrorResponse].error.message should not be empty
    }
  }

  test("list by file type and pattern - success code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Get("/list?type=Annex&pattern=.*.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[List[ConfigFileInfo]].size shouldBe 1
    }
  }

  // DEOPSCSW-577: Ability to view detailed change log in SVN
  // DEOPSCSW-625: Include username from svn in history model of config service
  test(
    "history - success  status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-577, DEOPSCSW-69, DEOPSCSW-625"
  ) {
    val bilal  = "bilal"
    val poorva = "poorva"

    when(validToken.preferred_username)
      .thenReturn(Some(bilal))
      .andThen(Some(poorva))

    when(validToken.userOrClientName)
      .thenReturn(bilal)
      .andThen(poorva)

    // consumes 2 revisions, one for actual file one for active file
    // first request will use username=bilal
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    val timeWhenFileWasCreated = Instant.now()

    // second request will use username=poorva
    Put("/config/test.conf?comment=commit2", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    val timeWhenFileWasUpdated = Instant.now()

    val configFileHistoryIdAuthorCommentTuples = Set((ConfigId(1), bilal, "commit1"), (ConfigId(3), poorva, "commit2"))

    Get("/history/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.author, history.comment))
        .toSet shouldEqual configFileHistoryIdAuthorCommentTuples
    }

    Get("/history/test.conf?maxResults=1") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.author, history.comment)) shouldEqual List((ConfigId(3), poorva, "commit2"))
    }

    Get(s"/history/test.conf?maxResults=1&from=$timeWhenFileWasCreated&to=$timeWhenFileWasUpdated") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.author, history.comment)) shouldEqual List((ConfigId(3), poorva, "commit2"))
    }

  }

  test("history - failure  status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

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

  test("getActive - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Get("/active-config/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[String] shouldEqual configValue1
    }
  }

  test("getActive - failure status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

    //  try to fetch active version of file which does not exists
    Get("/active-config/test1.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  test("getActive by date - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

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

  test("getActive by date - failure status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

    //  try to fetch active version of file which does not exists
    Get("/active-config/test1.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

    Get(s"/active-config?date=${Instant.now}") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  test("setActive - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

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

  test("setActive - failure status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

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

  test("resetActive - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

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

  test("resetActive - failure status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

    //  try to reset active version of file which does not exists
    Put("/active-version/test.conf").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }

  }

  // DEOPSCSW-577: Ability to view detailed change log in SVN
  // DEOPSCSW-625: Include username from svn in history model of config service
  test(
    "history-active - success  status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-577, DEOPSCSW-69, DEOPSCSW-625"
  ) {
    val bilal   = "bilal"
    val poorva  = "poorva"
    val shubham = "shubham"

    when(validToken.preferred_username)
      .thenReturn(Some(bilal))
      .andThen(Some(poorva))
      .andThen(Some(shubham))

    when(validToken.userOrClientName)
      .thenReturn(bilal)
      .andThen(poorva)
      .andThen(shubham)

    //consumes 2 revisions, one for actual file one for active file
    // first request will use username=bilal
    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    val timeWhenFileWasCreated = Instant.now()

    Put("/config/test.conf?comment=commit2", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    // second request will use username=poorva
    Put("/config/test.conf?comment=commit2", updatedConfigFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }

    // third request will use username=shubham
    Put("/active-version/test.conf?id=3&comment=commit1").addHeader(validTokenHeader) ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.OK
    }

    val timeWhenFileWasUpdated = Instant.now()

    val configFileHistoryIdAuthorCommentTuples =
      Set((ConfigId(1), bilal, "initializing active file with the first version"), (ConfigId(3), shubham, "commit1"))

    Get("/history-active/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.author, history.comment))
        .toSet shouldEqual configFileHistoryIdAuthorCommentTuples
    }

    Get("/history-active/test.conf?maxResults=1") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.author, history.comment)) shouldEqual List((ConfigId(3), shubham, "commit1"))
    }

    Get(s"/history-active/test.conf?maxResults=1&from=$timeWhenFileWasCreated&to=$timeWhenFileWasUpdated") ~> route ~> check {
      status shouldEqual StatusCodes.OK

      responseAs[List[ConfigFileRevision]]
        .map(history => (history.id, history.author, history.comment)) shouldEqual List((ConfigId(3), shubham, "commit1"))
    }

  }

  test("history-active - failure  status codes | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

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

  test("exists - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Created
    }

    Head("/config/test.conf") ~> route ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  test("exists - failure status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {

    Head("/config/test.conf?id=3") ~> route ~> check {
      status shouldEqual StatusCodes.NotFound
    }
    Head("/config/test_not_exists.conf") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  test("getMetadata - success status code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Get("/metadata") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[ConfigMetadata].repoPath should not be empty
      responseAs[ConfigMetadata].annexPath should not be empty
      responseAs[ConfigMetadata].annexMinFileSize should not be empty
      responseAs[ConfigMetadata].maxConfigFileSize should not be empty
    }
  }

  // DEOPSCSW-576: Auth token for Configuration service
  // DEOPSCSW-69: Use authorization token to get identity of user creating/updating a configuration file
  /** Auth Based routes * */
  /* ================ Unauthorized code ================*/
  test("create - Unauthorized code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Post("/config/test.conf?comment=create") ~> route ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  test("update - Unauthorized code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Put("/config/test.conf?comment=update") ~> route ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  test("delete - Unauthorized code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Delete("/config/test.conf?comment=deleting") ~> route ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  test("set active-version - Unauthorized code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Put("/active-version/test.conf?id=1&comment=active") ~> route ~> check {
      status shouldEqual StatusCodes.Unauthorized
    }
  }

  /* ================ Forbidden code ================*/
  test("create - Forbidden code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Post("/config/test.conf?comment=create").addHeader(roleMissingTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  test("update - Forbidden code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Put("/config/test.conf?comment=update").addHeader(roleMissingTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  test("set active-version - Forbidden code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Put("/active-version/test.conf?id=1&comment=active").addHeader(roleMissingTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  test("delete - Forbidden code | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69") {
    Delete("/config/test.conf?comment=deleting").addHeader(roleMissingTokenHeader) ~> route ~> check {
      status shouldEqual StatusCodes.Forbidden
    }
  }

  // DEOPSCSW-629: Token masking in logs
  test(
    "should mask authorization token while logging | DEOPSCSW-579, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-626, DEOPSCSW-69, DEOPSCSW-629"
  ) {
    val requests = ArrayBuffer.empty[HttpRequest]

    import serverWiring._
    val csRoute: ConfigServiceRoute =
      new ConfigServiceRoute(configServiceFactory, actorRuntime, configHandlers, serverWiring.securityDirectives) {
        override val logRequest: HttpRequest => Unit = maskedReq => requests += maskedReq
      }

    Post("/config/test.conf?annex=true&comment=commit1", configFile1).addHeader(validTokenHeader) ~> csRoute.route ~> check {
      status shouldEqual StatusCodes.Created
    }

    // the replacement text must match what is used in maskedToken in TokenMaskSupport.scala
    requests.head.header[Authorization].get.credentials.token() shouldBe "**********"
  }
}
