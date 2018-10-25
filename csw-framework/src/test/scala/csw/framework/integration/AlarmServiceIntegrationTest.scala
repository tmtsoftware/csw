package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.command.client.CommandServiceFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.models.framework.SupervisorLifecycleState
import csw.common.FrameworkAssertions.assertThatSupervisorIsRunning
import csw.common.components.framework.SampleComponentState._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.HCD
import csw.location.api.models.Connection.AkkaConnection
import csw.params.commands.Setup
import redis.embedded.{RedisSentinel, RedisServer}

import scala.concurrent.duration.DurationLong

//DEOPSCSW-490: Alarm service integration with framework
//DEOPSCSW-481: Component Developer API available to all CSW components
class AlarmServiceIntegrationTest extends FrameworkIntegrationSuite {
  import testWiring._

  private val masterId: String        = ConfigFactory.load().getString("csw-alarm.redis.masterId")
  private var sentinel: RedisSentinel = _
  private var server: RedisServer     = _

  private val wiring: FrameworkWiring = FrameworkWiring.make(testActorSystem)
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
    Http(testActorSystem).shutdownAllConnectionPools().await
    stopSentinel(sentinel, server)
    super.afterAll()
  }

  test("component should be able to set severity of an alarm") {
    import wiring._
    Standalone.spawn(ConfigFactory.load("standalone.conf"), wiring)

    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]("supervisor-lifecycle-state-probe")
    val akkaConnection                = AkkaConnection(ComponentId("IFS_Detector", HCD))
    val location                      = locationService.resolve(akkaConnection, 5.seconds).await

    val supervisorRef = location.get.componentRef
    assertThatSupervisorIsRunning(supervisorRef, supervisorLifecycleStateProbe, 5.seconds)

    val commandService = CommandServiceFactory.make(location.get)

    implicit val timeout: Timeout = Timeout(1000.millis)
    commandService.submit(Setup(prefix, setSeverityCommand, None)).await
    Thread.sleep(1000)

    adminAlarmService.getCurrentSeverity(testAlarmKey).await shouldEqual testSeverity
  }
}
