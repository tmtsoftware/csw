package csw.framework.deploy.hostconfig

import java.nio.file.Paths

import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-314: End to End Automated Test for host-config-app
class HostConfigTest extends FunSuite with MockitoSugar with Matchers {
  private val configPath            = Paths.get(getClass.getResource("/parsing_test_conf/hostconfig/valid_hostconfig.conf").getPath)
  private val containerCmdAppScript = "containerCmd.sh"
  private val mockedProcesses       = List(mock[Process], mock[Process])

  mockedProcesses.foreach(p ⇒ when(p.pid()).thenReturn(1))

  test("should parse host configuration file and invoke container cmd app with valid arguments") {
    var actualScripts: List[(String, List[String])] = Nil

    var counter = 0

    val hostConfig = new HostConfig("test") {
      override def executeScript(containerScript: String, args: String*): Process = {
        actualScripts = (containerScript, args.toList) :: actualScripts
        val process = mockedProcesses(counter)
        counter += 1
        process
      }
    }

    hostConfig.run(isLocal = true, Some(configPath), containerCmdAppScript)

    /*    ======== this is the host config file used in this test ========
      {
        mode: "Container"
        configFilePath: "/csw-framework/src/resources/laser_container.conf"
        configFileLocation: "Local"
      },
      {
        mode: "Standalone"
        configFilePath: "standalone.conf"
        configFileLocation: "Remote"
      }

     */

    val expectedScripts =
      List(
        (containerCmdAppScript, List("standalone.conf", "--standalone")),
        (containerCmdAppScript, List("/csw-framework/src/resources/laser_container.conf", "--local"))
      )

    // verify that two processes gets created for two containers
    // and once application is finished, those processes are exited
    mockedProcesses.foreach(p ⇒ verify(p, times(1)).pid())
    actualScripts shouldBe expectedScripts
  }
}
