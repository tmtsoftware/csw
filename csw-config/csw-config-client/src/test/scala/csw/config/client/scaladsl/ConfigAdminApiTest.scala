/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.config.client.scaladsl

import java.nio.file.{Files, Paths}
import csw.commons.ResourceReader
import csw.config.api.exceptions.{FileNotFound, InvalidInput, NotAllowed, Unauthorized}
import csw.config.api.scaladsl.ConfigService
import csw.config.api.{ConfigData, TokenFactory}
import csw.config.client.ConfigClientBaseSuite
import csw.config.models.FileType
import csw.config.server.commons.TestFutureExtension.RichFuture
import csw.config.server.files.Sha1
import csw.config.server.{ConfigServiceTest, ServerWiring}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import org.mockito.Mockito.when

// DEOPSCSW-138: Split Config API into Admin API and Client API
// DEOPSCSW-80: HTTP based access for configuration file
// DEOPSCSW-576: Auth token for Configuration service
// DEOPSCSW-88: Config service API implementation details need to be hidden from component developer
class ConfigAdminApiTest extends ConfigServiceTest with ConfigClientBaseSuite {

  override val serverWiring: ServerWiring = ServerWiring.make(securityDirectives)
  val actorRuntime                        = serverWiring.actorRuntime
  import actorRuntime._

  private val httpService           = serverWiring.httpService
  private val clientLocationService = HttpLocationServiceFactory.makeLocalClient

  override val configService: ConfigService = ConfigClientFactory.adminApi(typedSystem, clientLocationService, factory)

  override def beforeAll(): Unit = {
    super[ConfigClientBaseSuite].beforeAll()
    httpService.registeredLazyBinding.await
  }

  override def afterAll(): Unit = {
    httpService.shutdown().await
    super[ConfigServiceTest].afterAll()
    super[ConfigClientBaseSuite].afterAll()
  }

  // DEOPSCSW-47: Unique name for configuration file
  // DEOPSCSW-135: Validation of suffix for active and sha files
  test(
    "should throw exception for invalid path | DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-135, DEOPSCSW-47, DEOPSCSW-88, DEOPSCSW-138"
  ) {
    val filePath = Paths.get("/test/sample.$active")

    a[InvalidInput] shouldBe thrownBy(
      configService.create(filePath, ConfigData.fromString(configValue1), annex = false, "invalid path").await
    )
  }

  // DEOPSCSW-27: Storing binary component configurations
  // DEOPSCSW-81: Storing large files in the configuration service
  // DEOPSCSW-131: Detect and handle oversize files
  test(
    "should be able to store and retrieve binary file in annex dir | DEOPSCSW-131, DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-27, DEOPSCSW-81, DEOPSCSW-88, DEOPSCSW-138"
  ) {
    val fileName   = "smallBinary.bin"
    val path       = ResourceReader.copyToTmp("/" + fileName, ".bin")
    val configData = ConfigData.fromPath(path)
    val repoPath   = Paths.get(fileName)

    // verify that files smaller than annex-min-file-size go to annex if encoding is Binary
    serverWiring.settings.`annex-min-file-size` should be > configData.length

    val configId =
      configService.create(repoPath, configData, annex = false, s"committing file: $fileName").await

    val expectedContent = configService.getById(repoPath, configId).await.get.toInputStream.toByteArray
    expectedContent shouldBe Files.readAllBytes(path)

    val list = configService.list(Some(FileType.Annex)).await
    list.map(_.path) shouldBe List(repoPath)
    list.map(_.author) shouldBe List(preferredUserName)

    // Note that configService instance from the server-wiring can be used for assert-only calls for sha files
    // This call is invalid from client side
    val svnConfigData =
      serverConfigService
        .getById(Paths.get(s"$fileName${serverWiring.settings.`sha1-suffix`}"), configId)
        .await
        .get
    svnConfigData.toStringF.await shouldBe Sha1.fromConfigData(configData).await
  }

  // DEOPSCSW-75 List the names of configuration files that match a path
  test(
    "should throw invalid input exception if pattern is invalid | DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-75, DEOPSCSW-88, DEOPSCSW-138"
  ) {
    a[InvalidInput] shouldBe thrownBy(configService.list(pattern = Some("?i)")).await)
  }

  // DEOPSCSW-576: Auth token for Configuration service
  // DEOPSCSW-69: Use authorization token to get identity of user creating/updating a configuration file
  test(
    "should throw Unauthorized exception when not logged in | DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-69, DEOPSCSW-88, DEOPSCSW-138"
  ) {
    val invalidTFactory: TokenFactory = mock[TokenFactory]
    when(invalidTFactory.getToken).thenReturn("")

    val configServiceNotLoggedIn: ConfigService =
      ConfigClientFactory.adminApi(typedSystem, clientLocationService, invalidTFactory)

    val filePath = Paths.get("/test/sample")

    a[Unauthorized.type] shouldBe thrownBy(
      configServiceNotLoggedIn.create(filePath, ConfigData.fromString(configValue1), annex = false, "invalid path").await
    )
  }

  // DEOPSCSW-576: Auth token for Configuration service
  // DEOPSCSW-69: Use authorization token to get identity of user creating/updating a configuration file
  test(
    "should throw NotAllowed exception when user does not have correct role | DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-69, DEOPSCSW-88, DEOPSCSW-138"
  ) {
    val invalidTFactory: TokenFactory = mock[TokenFactory]
    when(invalidTFactory.getToken).thenReturn(roleMissingTokenStr)

    val configServiceIncorrectRole: ConfigService =
      ConfigClientFactory.adminApi(typedSystem, clientLocationService, invalidTFactory)

    val filePath = Paths.get("/test/sample")

    a[NotAllowed.type] shouldBe thrownBy(
      configServiceIncorrectRole.create(filePath, ConfigData.fromString(configValue1), annex = false, "invalid path").await
    )
  }

  // DEOPSCSW-577: Ability to view detailed change log in SVN
  // DEOPSCSW-625: Include username from svn in history model of config service
  test(
    "should get the history of a file with correct username | DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-577, DEOPSCSW-625, DEOPSCSW-88, DEOPSCSW-138"
  ) {
    val user1 = "user1"
    val user2 = "user2"
    val user3 = "user3"
    when(validToken.preferred_username).thenReturn(Some(user1), Some(user2), Some(user3))
    when(validToken.userOrClientName).thenReturn(user1, user2, user3)

    val file = Paths.get("/tmt/lgs/trombone/hcd.conf")

    a[FileNotFound] shouldBe thrownBy(configService.history(file).await)

    val commitMsg1 = "commit version: 1"
    val commitMsg2 = "commit version: 2"
    val commitMsg3 = "commit version: 3"

    // will use user1
    val configId1 = configService.create(file, ConfigData.fromString(configValue1), annex = false, commitMsg1).await

    // will use user2
    val configId2 = configService.update(file, ConfigData.fromString(configValue2), commitMsg2).await

    // will use user3
    val configId3 = configService.update(file, ConfigData.fromString(configValue3), commitMsg3).await

    // verify history without any parameter
    val configFileHistories = configService.history(file).await
    configFileHistories.size shouldBe 3
    configFileHistories.map(h => (h.id, h.author, h.comment)).toSet shouldBe
    Set((configId1, user1, commitMsg1), (configId2, user2, commitMsg2), (configId3, user3, commitMsg3))
  }

  // DEOPSCSW-577: Ability to view detailed change log in SVN
  // DEOPSCSW-625: Include username from svn in history model of config service
  test(
    "should able to get history of active versions of file with correct user names | DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-577, DEOPSCSW-625, DEOPSCSW-88, DEOPSCSW-138"
  ) {
    val user1 = "user1"
    val user2 = "user2"
    val user3 = "user3"
    val user4 = "user4"
    val user5 = "user5"
    when(validToken.preferred_username).thenReturn(Some(user1), Some(user2), Some(user3), Some(user4), Some(user5))
    when(validToken.userOrClientName).thenReturn(user1, user2, user3, user4, user5)

    val file = Paths.get("/tmt/test/setactive/getactive/resetactive/active.conf")
    // on create call, active file gets created with this comment by default, note that this is an internal implementation
    val createActiveComment = "initializing active file with the first version"

    // create file (user1)
    val configId1 = configService.create(file, ConfigData.fromString(configValue1), annex = false, "create").await

    // update file twice
    // user2
    val configId3 = configService.update(file, ConfigData.fromString(configValue3), "Update 1").await
    // user3
    val configId4 = configService.update(file, ConfigData.fromString(configValue4), "Update 2").await

    // set active version of file to id=3
    val setActiveComment = "Setting active version for the first time"
    configService.setActiveVersion(file, configId3, setActiveComment).await // user4

    // reset active version and check that get active returns latest version
    val resetActiveComment1 = "resetting active version"
    configService.resetActiveVersion(file, resetActiveComment1).await // user5

    // verify complete history of active file without any parameters
    val completeHistory = configService.historyActive(file).await
    completeHistory.size shouldBe 3
    completeHistory.map(h => (h.id, h.author, h.comment)).toSet shouldBe
    Set((configId1, user1, createActiveComment), (configId3, user4, setActiveComment), (configId4, user5, resetActiveComment1))
  }

  // DEOPSCSW-577: Ability to view detailed change log in SVN
  test(
    "should get the list of a file with correct username | DEOPSCSW-80, DEOPSCSW-576, DEOPSCSW-577, DEOPSCSW-88, DEOPSCSW-138"
  ) {
    val user1 = "user1"
    val user2 = "user2"
    when(validToken.preferred_username).thenReturn(Some(user1), Some(user2))
    when(validToken.userOrClientName).thenReturn(user1, user2)

    val file1 = Paths.get("/tmt/lgs/trombone/hcd1.conf")
    val file2 = Paths.get("/tmt/lgs/trombone/hcd2.conf")

    val commitMsg1 = "commit version: 1"
    val commitMsg2 = "commit version: 2"

    // will use user1
    val configId1 = configService.create(file1, ConfigData.fromString(configValue1), annex = false, commitMsg1).await

    // will use user2
    val configId2 = configService.create(file2, ConfigData.fromString(configValue2), annex = false, commitMsg2).await

    val fileInfo       = configService.list().await
    val expectedResult = Set((configId1, user1, commitMsg1), (configId2, user2, commitMsg2))

    fileInfo.size shouldBe expectedResult.size
    fileInfo.map(h => (h.id, h.author, h.comment)).toSet shouldBe expectedResult
  }

}
