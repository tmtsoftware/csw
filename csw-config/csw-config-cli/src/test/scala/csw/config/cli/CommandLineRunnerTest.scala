package csw.config.cli

import java.nio.file.{Files, Paths}
import java.time.Instant

import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.stream.ActorMaterializer
import csw.aas.native.api.NativeAppAuthAdapter
import csw.config.api.models.ConfigId
import csw.config.cli.args.{ArgsParser, Options}
import csw.config.cli.wiring.Wiring
import csw.config.commons.TestFutureExtension.RichFuture
import csw.config.commons.{ArgsUtil, TestFileUtils}
import csw.config.server.ServerWiring
import csw.config.server.mocks.MockedAuthentication
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.http.HTTPLocationService
import org.mockito.Mockito.{verify, when}
import org.scalatest.{BeforeAndAfterEach, Matchers}

// DEOPSCSW-112: Command line interface client for Configuration service
// DEOPSCSW-43: Access Configuration service from any CSW component
// DEOPSCSW-576: Auth token for Configuration service
class CommandLineRunnerTest extends HTTPLocationService with Matchers with BeforeAndAfterEach with MockedAuthentication {

  private val clientSystem: ActorSystem               = ActorSystem("config-cli")
  private val clientMat: ActorMaterializer            = ActorMaterializer()(clientSystem)
  private val locationService                         = HttpLocationServiceFactory.makeLocalClient(clientSystem, clientMat)
  private val nativeAuthAdapter: NativeAppAuthAdapter = mock[NativeAppAuthAdapter]
  private val clientWiring                            = Wiring.noPrinting(locationService, factory, nativeAuthAdapter)

  private val serverWiring = ServerWiring.make(locationService, securityDirectives)

  import clientWiring.commandLineRunner

  private val testFileUtils = new TestFileUtils(serverWiring.settings)

  val ArgsUtil = new ArgsUtil
  import ArgsUtil._

  val argsParser = new ArgsParser("csw-config-cli")

  override def beforeAll(): Unit = {
    super.beforeAll()
    testFileUtils.deleteServerFiles()
    serverWiring.httpService.registeredLazyBinding.await
  }

  override def beforeEach(): Unit = serverWiring.svnRepo.initSvnRepo()
  override def afterEach(): Unit = {
    testFileUtils.deleteServerFiles()
    if (Files.exists(Paths.get(outputFilePath))) Files.delete(Paths.get(outputFilePath))
  }
  override def afterAll(): Unit = {
    serverWiring.httpService.shutdown(UnknownReason).await
    clientWiring.actorRuntime.shutdown(UnknownReason).await
    serverWiring.actorRuntime.shutdown(UnknownReason).await
    Files.delete(Paths.get(inputFilePath))
    Files.delete(Paths.get(updatedInputFilePath))
    super.afterAll()
  }

  //DEOPSCSW-72: Retrieve a configuration file to a specified file location on a local disk
  test("should able to create a file in repo and read it from repo to local disk") {

    commandLineRunner.create(argsParser.parse(createMinimalArgs).get) shouldBe ConfigId(1)

    commandLineRunner.get(argsParser.parse(getLatestArgs).get) shouldBe Some(Paths.get(outputFilePath))

    // read locally saved output file (/tmp/output.conf) from disk and
    // match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents
  }

  test("should able to update, delete and check for existence of a file from repo") {

    commandLineRunner.create(argsParser.parse(createMinimalArgs).get)

    val getByDateArgs = Array("get", relativeRepoPath, "-o", outputFilePath, "--date", Instant.now.toString)

    val updateConfigId = commandLineRunner.update(argsParser.parse(updateAllArgs).get)

    commandLineRunner.get(argsParser.parse(getLatestArgs).get) shouldBe Some(Paths.get(outputFilePath))

    commandLineRunner.get(argsParser.parse(getByIdArgs :+ updateConfigId.id).get) shouldBe Some(
      Paths.get(outputFilePath)
    )

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents

    commandLineRunner.get(argsParser.parse(getByDateArgs).get) shouldBe Some(Paths.get(outputFilePath))

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents

    //  file should exist
    val parsedExistsArgs1: Option[Options] = argsParser.parse(existsArgs)
    commandLineRunner.exists(parsedExistsArgs1.get) shouldBe true

    //  delete file
    val parsedDeleteArgs: Option[Options] = argsParser.parse(deleteArgs)
    commandLineRunner.delete(parsedDeleteArgs.get)

    //  deleted file should not exist
    val parsedExistsArgs: Option[Options] = argsParser.parse(existsArgs)
    commandLineRunner.exists(parsedExistsArgs.get) shouldBe false
  }

  // DEOPSCSW-577: Ability to view detailed change log in SVN
  test("should be able to list files with author and use filter pattern") {
    val normalFileName                                = "troubleshooting"
    val annexFileName                                 = "firmware"
    def relativeRepoPath(fileName: String, i: String) = s"path/hcd/$fileName$i.conf"

    //  create 4 normal files
    val normalFiles = for {
      i ← 1 to 4
    } yield relativeRepoPath(normalFileName, i.toString)
    normalFiles.map { fileName ⇒
      commandLineRunner.create(argsParser.parse(Array("create", fileName, "-i", inputFilePath, "-c", comment)).get)
    }

    //  create 3 annex files
    val annexFiles = for {
      i ← 1 to 3
    } yield relativeRepoPath(annexFileName, i.toString)
    annexFiles.map { fileName ⇒
      commandLineRunner
        .create(argsParser.parse(Array("create", fileName, "-i", inputFilePath, "--annex", "-c", comment)).get)
    }

    commandLineRunner.list(argsParser.parse(Array("list", "--annex", "--normal")).get) shouldBe empty

    commandLineRunner
      .list(argsParser.parse(Array("list", "--normal")).get)
      .map(x ⇒ (x.path.toString, x.author))
      .toSet shouldBe normalFiles.map(x ⇒ (x, preferredUserName)).toSet

    commandLineRunner
      .list(argsParser.parse(Array("list", "--annex")).get)
      .map(_.path.toString)
      .toSet shouldBe annexFiles.toSet

    commandLineRunner
      .list(argsParser.parse(Array("list", "--pattern", "/path/hcd/*.*")).get)
      .map(_.path.toString)
      .toSet shouldBe (annexFiles ++ normalFiles).toSet
  }

  test("should able to set, reset and get the active version of file.") {

    commandLineRunner.create(argsParser.parse(createMinimalArgs).get)

    val updateConfigId = commandLineRunner.update(argsParser.parse(updateAllArgs).get)

    //  set active version of file to id=1 and store it at location: /tmp/output.txt
    val parsedSetActiveArgs: Option[Options] = argsParser.parse(setActiveAllArgs)
    commandLineRunner.setActiveVersion(parsedSetActiveArgs.get)

    val getByDateArgs =
      Array("getActiveByTime", relativeRepoPath, "-o", outputFilePath, "--date", Instant.now.toString)

    //  get active version of file and store it at location: /tmp/output.txt
    val parsedGetActiveArgs: Option[Options] = argsParser.parse(getMinimalArgs)
    commandLineRunner.getActiveVersion(parsedGetActiveArgs.get).get shouldBe ConfigId(parsedSetActiveArgs.get.id.get)
    commandLineRunner.getActive(parsedGetActiveArgs.get) shouldBe Some(Paths.get(outputFilePath))

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents

    commandLineRunner.resetActiveVersion(argsParser.parse(resetActiveAllArgs).get)

    //  get active version of file and store it at location: /tmp/output.txt
    commandLineRunner.getActiveVersion(parsedGetActiveArgs.get).get shouldBe updateConfigId
    commandLineRunner.getActive(parsedGetActiveArgs.get) shouldBe Some(Paths.get(outputFilePath))

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual updatedInputFileContents

    commandLineRunner.getActiveByTime(argsParser.parse(getByDateArgs).get) shouldBe Some(Paths.get(outputFilePath))

    //  read locally saved output file (/tmp/output.conf) from disk and
    //  match the contents with input file content
    readFile(outputFilePath) shouldEqual inputFileContents
  }

  // DEOPSCSW-577: Ability to view detailed change log in SVN
  // DEOPSCSW-625: Include username from svn in history model of config service
  test("should able to fetch history of file.") {
    val user1 = "user1"
    val user2 = "user2"
    val user3 = "user3"
    when(validToken.preferred_username).thenReturn(Some(user1), Some(user2), Some(user3))
    when(validToken.userOrClientName).thenReturn(user1, user2, user3)

    //  create file
    val parsedCreateArgs: Option[Options] = argsParser.parse(createMinimalArgs)
    val createConfigId                    = commandLineRunner.create(parsedCreateArgs.get)
    val createTS                          = Instant.now

    //  update file content
    val parsedUpdateArgs: Option[Options] = argsParser.parse(updateAllArgs)
    val updateConfigId                    = commandLineRunner.update(parsedUpdateArgs.get)

    //  update file content
    val parsedUpdate2Args: Option[Options] = argsParser.parse(updateAllArgs)
    val update2ConfigId                    = commandLineRunner.update(parsedUpdate2Args.get)
    val updateTS                           = Instant.now

    commandLineRunner.history(argsParser.parse(historyArgs).get).map(h ⇒ (h.id, h.author, h.comment)).toSet shouldBe
    Set((createConfigId, user1, comment), (updateConfigId, user2, comment), (update2ConfigId, user3, comment))

    commandLineRunner
      .history(argsParser.parse(historyArgs :+ "--from" :+ createTS.toString :+ "--to" :+ updateTS.toString).get)
      .map(_.id) shouldBe List(update2ConfigId, updateConfigId)
  }

  // DEOPSCSW-577: Ability to view detailed change log in SVN
  // DEOPSCSW-625: Include username from svn in history model of config service
  test("should able to fetch history of active files.") {
    val user1 = "user1"
    val user2 = "user2"
    val user3 = "user3"
    val user4 = "user4"
    val user5 = "user5"
    when(validToken.preferred_username).thenReturn(Some(user1), Some(user2), Some(user3), Some(user4), Some(user5))
    when(validToken.userOrClientName).thenReturn(user1, user2, user3, user4, user5)

    //  create file
    val parsedCreateArgs: Option[Options] = argsParser.parse(createMinimalArgs)
    val createConfigId                    = commandLineRunner.create(parsedCreateArgs.get) //user1
    val createTS                          = Instant.now

    //  update file content
    val parsedUpdateArgs: Option[Options] = argsParser.parse(updateAllArgs)
    val updateConfigId                    = commandLineRunner.update(parsedUpdateArgs.get) //user2

    //  update file content
    val parsedUpdate2Args: Option[Options] = argsParser.parse(updateAllArgs)
    val update2ConfigId                    = commandLineRunner.update(parsedUpdate2Args.get) //user3

    val setActiveArgs = Array("setActiveVersion", relativeRepoPath, "--id", updateConfigId.id, "-c", comment)
    commandLineRunner.setActiveVersion(argsParser.parse(setActiveArgs).get) //user4

    commandLineRunner.historyActive(argsParser.parse(historyActiveArgs).get).map(h ⇒ (h.id, h.author, h.comment)).toSet shouldBe
    Set((createConfigId, user1, "initializing active file with the first version"), (updateConfigId, user4, comment))

    commandLineRunner.resetActiveVersion(argsParser.parse(resetActiveAllArgs).get) //user5

    commandLineRunner
      .historyActive(
        argsParser.parse(historyActiveArgs :+ "--from" :+ createTS.toString :+ "--to" :+ Instant.now.toString).get
      )
      .map(_.id) shouldBe List(update2ConfigId, updateConfigId)
  }

  test("get repository MetaData from server") {
    val parsedMetaDataArgs: Option[Options] = argsParser.parse(meteDataArgs)
    val actualMetadata                      = commandLineRunner.getMetadata(parsedMetaDataArgs.get)
    actualMetadata.toString should not be empty
  }

  // DEOPSCSW-576: Auth token for Configuration service
  // DEOPSCSW-69: Use authorization token to get identity of user creating/updating a configuration file
  // DEOPSCSW-65: Support name or role of configuration file creator/updater
  test("login") {
    commandLineRunner.login()
    verify(nativeAuthAdapter).login()
  }

  // DEOPSCSW-576: Auth token for Configuration service
  test("logout") {
    commandLineRunner.logout()
    verify(nativeAuthAdapter).logout()
  }

}
