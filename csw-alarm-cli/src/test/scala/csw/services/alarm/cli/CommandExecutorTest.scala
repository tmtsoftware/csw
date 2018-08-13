package csw.services.alarm.cli

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.alarm.api.models.Key.GlobalKey
import csw.services.alarm.cli.args.CommandLineArgs
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.config.api.models.ConfigData
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.{ServerWiring, Settings}
import csw.services.config.server.commons.TestFileUtils
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

class CommandExecutorTest extends FunSuite with Matchers with SeedData with BeforeAndAfterAll {

  import cliWiring._
  import actorRuntime._

  private val adminService = alarmAdminClient.alarmServiceF.await

  override def afterAll(): Unit = {
    val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))
    testFileUtils.deleteServerFiles()
    shutdown(TestFinishedReason).await
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should initialize alarms in alarm store from local config") {
    val filePath = Paths.get(getClass.getResource("/valid-alarms.conf").getPath)
    val args     = CommandLineArgs("init", Some(filePath), isLocal = true)
    commandExecutor.execute(args)

    logBuffer should contain("[SUCCESS] Alarms successfully initialized.")

    val metadata = adminService.getMetadata(GlobalKey).await

    metadata.map(_.alarmKey.value).toSet shouldEqual Set(
      "nfiraos.trombone.tromboneaxishighlimitalarm",
      "nfiraos.trombone.tromboneaxislowlimitalarm",
      "tcs.tcspk.cpuexceededalarm",
      "lgsf.tcspkinactive.cpuidlealarm"
    )
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should initialize alarms in alarm store from remote config") {
    val serverWiring = ServerWiring.make(locationService)
    serverWiring.svnRepo.initSvnRepo()
    serverWiring.httpService.registeredLazyBinding.await

    val configData    = ConfigData.fromPath(Paths.get(getClass.getResource("/valid-alarms.conf").getPath))
    val configPath    = Paths.get("valid-alarms.conf")
    val configService = ConfigClientFactory.adminApi(system, locationService)
    configService.create(configPath, configData, comment = "commit test file").await

    val args = CommandLineArgs("init", Some(configPath))
    commandExecutor.execute(args)

    logBuffer should contain("[SUCCESS] Alarms successfully initialized.")

    val metadata = adminService.getMetadata(GlobalKey).await

    metadata.map(_.alarmKey.value).toSet shouldEqual Set(
      "nfiraos.trombone.tromboneaxishighlimitalarm",
      "nfiraos.trombone.tromboneaxislowlimitalarm",
      "tcs.tcspk.cpuexceededalarm",
      "lgsf.tcspkinactive.cpuidlealarm"
    )

    configService.delete(configPath, "deleting test file").await
  }

  // DEOPSCSW-470: CLI application to exercise and test the alarm API
  test("should fail to initialize alarms in alarm store when config service is down") {

    val configData = ConfigData.fromPath(Paths.get(getClass.getResource("/valid-alarms.conf").getPath))
    val configPath = Paths.get("valid-alarms.conf")

    val args = CommandLineArgs("init", Some(configPath))
    intercept[RuntimeException] {
      commandExecutor.execute(args)
    }

    logBuffer should contain(
      "[FAILURE] Failed to initialize alarm store with error: [File does not exist at path=valid-alarms.conf]"
    )
  }

}
