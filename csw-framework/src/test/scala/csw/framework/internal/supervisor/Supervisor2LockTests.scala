package csw.framework.internal.supervisor

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.command.client.CommandResponseManager
import csw.command.client.messages.ComponentCommonMessage.GetSupervisorLifecycleState
import csw.command.client.messages.SupervisorMessage
import csw.command.client.models.framework.{ComponentInfo, SupervisorLifecycleState}
import csw.command.client.models.framework.LocationServiceUsage.RegisterOnly
import csw.command.client.models.framework.SupervisorLifecycleState.{Idle, Running}
import csw.common.components.command
import csw.framework.FrameworkTestMocks
import csw.framework.integration.MyFrameworkMocks
import csw.framework.models.CswContext
import csw.location.api.models.ComponentType.Assembly
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuite, AnyFunSuiteLike}
import csw.logging.client.scaladsl.LoggerFactory
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Subsystem.ESW
import csw.time.scheduler.TimeServiceSchedulerFactory
import org.mockito.MockitoSugar.mock

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationDouble

class MyFrameworkMocks {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "testSystem")
  def frameworkTestMocks(): FrameworkTestMocks                 = new FrameworkTestMocks()

  //private val manualTime                    = ManualTime()(system)
  private val jitter                        = 10
  private implicit val scheduler: Scheduler = typedSystem.scheduler
  private implicit val ec: ExecutionContext = typedSystem.executionContext

  def createContext(componentInfo: ComponentInfo): CswContext = {
    val mocks = frameworkTestMocks()
    new CswContext(
      mocks.locationService,
      mocks.eventService,
      mocks.alarmService,
      new TimeServiceSchedulerFactory().make(),
      new LoggerFactory(componentInfo.prefix),
      mocks.configClientService,
      mocks.currentStatePublisher,
      mock[CommandResponseManager],
      componentInfo
    )
  }
}


class Supervisor2LockTests extends ScalaTestWithActorTestKit with AnyFunSuiteLike with MockitoSugar with BeforeAndAfterAll {
  private val clientPrefix:Prefix = Prefix(ESW, "engUI")
  private val invalidPrefix = Prefix("wfos.invalid.engUI")

  val assemblyInfo: ComponentInfo = ComponentInfo(
    Prefix("WFOS.SampleAssembly"),
    Assembly,
    "csw.common.components.framework.SampleComponentBehaviorFactory",
    RegisterOnly,
    Set.empty
  )

  override def afterAll(): Unit = testKit.shutdownTestKit()

  private val mockedLoggerFactory      = mock[LoggerFactory]
  private val mockedLogger             = mock[Logger]
  when(mockedLoggerFactory.getLogger).thenReturn(mockedLogger)

  test("should be unlocked when prefix is not available | DEOPSCSW-222, DEOPSCSW-301") {

    val testMocks = new MyFrameworkMocks()
    val cswContext = testMocks.createContext(assemblyInfo)
    val testSuper =
      spawn(SupervisorBehavior2(command.TestComponent(cswContext), testMocks.frameworkTestMocks().registrationFactory, cswContext))

    val testProbe = TestProbe[SupervisorMessage]()
    //val isLockedProbe = testKit.createTestProbe[LockManager2Response]
    //val lockManager2ReponseProbe = testKit.createTestProbe[LockManager2Response]
    val lifecycleStateProbe = testKit.createTestProbe[SupervisorLifecycleState]()

    testSuper ! GetSupervisorLifecycleState(lifecycleStateProbe.ref)

    lifecycleStateProbe.expectMessage(Idle)

    Thread.sleep(1000)

    testSuper ! GetSupervisorLifecycleState(lifecycleStateProbe.ref)
    lifecycleStateProbe.expectMessage(Running)


    // Check for unhandled
    //lm ! LockPrefix(lockManager2ReponseProbe.ref)
    //lockManager2ReponseProbe.expectMessage(Unhandled)
  }
}
