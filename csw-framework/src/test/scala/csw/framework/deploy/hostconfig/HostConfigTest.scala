/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.deploy.hostconfig

import csw.commons.ResourceReader
import csw.prefix.models.Subsystem.CSW
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

// DEOPSCSW-174: Starting Multiple Containers with an init Script
// DEOPSCSW-314: End to End Automated Test for host-config-app
class HostConfigTest extends AnyFunSuite with MockitoSugar with Matchers {

  private val configPath            = ResourceReader.copyToTmp("/parsing_test_conf/hostconfig/valid_hostconfig.conf")
  private val containerCmdAppScript = "containerCmd.sh"
  private val mockedProcesses       = List(mock[Process], mock[Process])

  mockedProcesses.foreach(p => when(p.pid()).thenReturn(1))

  test(
    "should parse host configuration file and invoke container cmd app with valid arguments | DEOPSCSW-174, DEOPSCSW-314, CSW-177"
  ) {
    var actualScripts: List[(String, List[String])] = Nil

    var counter = 0

    val hostConfig = new HostConfig("test", CSW) {
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
        configFilePath: "/csw-framework/src/resources/laser_container.conf"
        configFileLocation: "Local"
      },
      {
        configFilePath: "standalone.conf"
        configFileLocation: "Remote"
      }

     */

    val expectedScripts =
      List(
        (containerCmdAppScript, List("standalone.conf")),
        (containerCmdAppScript, List("/csw-framework/src/resources/laser_container.conf", "--local"))
      )

    // verify that two processes gets created for two containers
    // and once application is finished, those processes are exited
    mockedProcesses.foreach(p => verify(p, times(1)).pid())
    actualScripts shouldBe expectedScripts
  }
}
