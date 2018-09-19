package csw.common

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentCommonMessage.GetSupervisorLifecycleState
import csw.command.client.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.command.client.messages.{ComponentMessage, ContainerMessage}
import csw.command.client.models.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.logging.internal.JsonExtensions.RichJsObject
import csw.logging.internal.LoggingLevels.Level
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import play.api.libs.json.{JsObject, JsString}

import scala.collection.mutable
import scala.concurrent.duration.Duration

object FrameworkAssertions extends Matchers with Eventually {

  def assertThatContainerIsRunning(
      containerRef: ActorRef[ContainerMessage],
      probe: TestProbe[ContainerLifecycleState],
      duration: Duration
  ): Unit = {
    def getContainerLifecycleState: ContainerLifecycleState = {
      containerRef ! GetContainerLifecycleState(probe.ref)
      probe.expectMessageType[ContainerLifecycleState]
    }

    eventually(timeout(duration))(
      assert(
        getContainerLifecycleState == ContainerLifecycleState.Running,
        s"expected :${ContainerLifecycleState.Running}, found :$getContainerLifecycleState"
      )
    )
  }

  def assertThatSupervisorIsRunning(
      actorRef: ActorRef[ComponentMessage],
      probe: TestProbe[SupervisorLifecycleState],
      duration: Duration
  ): Unit = {
    def getSupervisorLifecycleState: SupervisorLifecycleState = {
      actorRef ! GetSupervisorLifecycleState(probe.ref)
      probe.expectMessageType[SupervisorLifecycleState]
    }

    eventually(timeout(duration))(
      assert(
        getSupervisorLifecycleState == SupervisorLifecycleState.Running,
        s"expected :${SupervisorLifecycleState.Running}, found :$getSupervisorLifecycleState"
      )
    )
  }

  def assertThatMessageIsLogged(
      logBuffer: mutable.Buffer[JsObject],
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
      logBuffer: mutable.Buffer[JsObject],
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
    val traceBlock    = logMsg("trace").asInstanceOf[JsObject]
    val traceMsgBlock = traceBlock("message").as[JsObject]
    traceMsgBlock("ex") shouldBe s"class ${sanitizeClassName(exceptionClassName)}"
    traceMsgBlock("message") shouldBe exceptionMessage
  }

  def assertThatExceptionIsNotLogged(
      logBuffer: mutable.Buffer[JsObject],
      message: String,
  ): Unit = {
    val maybeLogMsg = findLogMessageSubString(logBuffer, message)
    assert(maybeLogMsg.isEmpty, s"$message found in $logBuffer")
  }

  private def findLogMessage(logBuffer: mutable.Buffer[JsObject], message: String): Option[JsObject] =
    logBuffer.find(_.value.exists {
      case (_, JsString(`message`)) ⇒ true
      case (_, _)                   ⇒ false
    })

  private def findLogMessageSubString(logBuffer: mutable.Buffer[JsObject], message: String): Option[JsObject] =
    logBuffer.find(_.value.exists {
      case (_, JsString(actualMessage)) if actualMessage.startsWith(message) ⇒ true
      case (_, _)                                                            ⇒ false
    })

  private def sanitizeClassName(className: String): String =
    if (className.endsWith("$")) className.dropRight(1) else className
}
