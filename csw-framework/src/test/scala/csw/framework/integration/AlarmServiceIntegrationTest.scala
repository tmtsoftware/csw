/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.integration

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.PekkoLocationExt.RichPekkoLocation
import csw.command.client.models.framework.SupervisorLifecycleState
import csw.common.FrameworkAssertions.assertThatSupervisorIsRunning
import csw.common.components.framework.SampleComponentState._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.location.api.models
import csw.location.api.models.ComponentType.HCD
import csw.location.api.models.Connection.PekkoConnection
import csw.params.commands.Setup
import csw.prefix.models.{Prefix, Subsystem}
import redis.embedded.{RedisSentinel, RedisServer}

import scala.concurrent.duration.DurationLong

//DEOPSCSW-490: Alarm service integration with framework
//DEOPSCSW-481: Component Developer API available to all CSW components
//CSW-82: ComponentInfo should take prefix
//CSW-83: Alarm models should take prefix
class AlarmServiceIntegrationTest extends FrameworkIntegrationSuite {
  import testWiring._

  private val masterId: String        = ConfigFactory.load().getString("csw-alarm.redis.masterId")
  private var sentinel: RedisSentinel = _
  private var server: RedisServer     = _

  private val wiring: FrameworkWiring = FrameworkWiring.make(seedActorSystem)
  private val adminAlarmService       = wiring.alarmServiceFactory.makeAdminApi(wiring.locationService)

  override def beforeAll(): Unit = {
    super.beforeAll()

    val tuple = startSentinelAndRegisterService(AlarmServiceConnection.value, masterId)
    sentinel = tuple._2
    server = tuple._3
    val config: Config = ConfigFactory.parseResources("valid-alarms.conf")
    adminAlarmService.initAlarms(config, reset = true).await
  }

  override def afterAll(): Unit = {
    stopSentinel(sentinel, server)
    super.afterAll()
  }

  test("component should be able to set severity of an alarm | DEOPSCSW-490, DEOPSCSW-481") {
    import wiring._
    Standalone.spawn(ConfigFactory.load("standalone.conf"), wiring)

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]("supervisor-lifecycle-state-probe")
    val pekkoConnection               = PekkoConnection(models.ComponentId(Prefix(Subsystem.IRIS, "IFS_Detector"), HCD))
    val location                      = locationService.resolve(pekkoConnection, 5.seconds).await

    val supervisorRef = location.get.componentRef
    assertThatSupervisorIsRunning(supervisorRef, supervisorLifecycleStateProbe, 5.seconds)

    val commandService = CommandServiceFactory.make(location.get)

    implicit val timeout: Timeout = Timeout(1000.millis)
    commandService.submitAndWait(Setup(prefix, setSeverityCommand, None)).await
    Thread.sleep(1000)

    adminAlarmService.getCurrentSeverity(testAlarmKey).await shouldEqual testSeverity
  }
}
