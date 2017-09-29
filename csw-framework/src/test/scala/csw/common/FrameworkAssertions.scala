package csw.common

import akka.typed.ActorRef
import akka.typed.testkit.scaladsl.TestProbe
import com.persist.JsonOps.JsonObject
import csw.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.messages.SupervisorCommonMessage.GetSupervisorLifecycleState
import csw.messages.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.messages.{ContainerExternalMessage, SupervisorExternalMessage}
import csw.services.location.commons.BlockingUtils
import csw.services.logging.internal.LoggingLevels.Level
import org.scalatest.Matchers

import scala.collection.mutable
import scala.concurrent.duration.Duration

object FrameworkAssertions extends Matchers {

  def assertThatContainerIsRunning(
      containerRef: ActorRef[ContainerExternalMessage],
      probe: TestProbe[ContainerLifecycleState],
      duration: Duration
  ): Unit = {
    def getContainerLifecycleState: ContainerLifecycleState = {
      containerRef ! GetContainerLifecycleState(probe.ref)
      probe.expectMsgType[ContainerLifecycleState]
    }

    assert(
      BlockingUtils.poll(getContainerLifecycleState == ContainerLifecycleState.Running, duration),
      s"expected :${ContainerLifecycleState.Running}, found :$getContainerLifecycleState"
    )
  }

  def assertThatSupervisorIsRunning(
      actorRef: ActorRef[SupervisorExternalMessage],
      probe: TestProbe[SupervisorLifecycleState],
      duration: Duration
  ): Unit = {
    def getSupervisorLifecycleState: SupervisorLifecycleState = {
      actorRef ! GetSupervisorLifecycleState(probe.ref)
      probe.expectMsgType[SupervisorLifecycleState]
    }

    assert(
      BlockingUtils.poll(getSupervisorLifecycleState == SupervisorLifecycleState.Running, duration),
      s"expected :${SupervisorLifecycleState.Running}, found :$getSupervisorLifecycleState"
    )
  }

  def assertThatMessageIsLogged(
      logBuffer: mutable.Buffer[JsonObject],
      componentName: String,
      message: String,
      expLevel: Level,
      className: String
  ): Unit = {

    val maybeLogMsg = findLogMessage(logBuffer, message)

    assert(maybeLogMsg.isDefined, s"$message not found in $logBuffer")
    val logMsg = maybeLogMsg.get
    Level(logMsg("@severity").toString) shouldBe expLevel
    logMsg("@componentName") shouldBe componentName
    logMsg("class") shouldBe sanitizeClassName(className)
  }

  def assertThatExceptionIsLogged(
      logBuffer: mutable.Buffer[JsonObject],
      componentName: String,
      message: String,
      expLevel: Level,
      className: String,
      exceptionClassName: String,
      exceptionMessage: String
  ): Unit = {

    val maybeLogMsg = findLogMessage(logBuffer, message)

    assert(maybeLogMsg.isDefined, s"$message not found in $logBuffer")
    val logMsg = maybeLogMsg.get
    Level(logMsg("@severity").toString) shouldBe expLevel
    logMsg("@componentName") shouldBe componentName
    logMsg("class") shouldBe sanitizeClassName(className)

    logMsg.contains("trace") shouldBe true
    val traceBlock    = logMsg("trace").asInstanceOf[JsonObject]
    val traceMsgBlock = traceBlock("message").asInstanceOf[JsonObject]
    traceMsgBlock("ex") shouldBe s"class ${sanitizeClassName(exceptionClassName)}"
    traceMsgBlock("message") shouldBe exceptionMessage
  }

  private def findLogMessage(logBuffer: mutable.Buffer[JsonObject], message: String): Option[JsonObject] =
    logBuffer.find(_.exists {
      case (_, `message`) ⇒ true
      case (_, _)         ⇒ false
    })

  private def sanitizeClassName(className: String): String =
    if (className.endsWith("$")) className.dropRight(1) else className
}
