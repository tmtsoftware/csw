package csw.logging.client.scaladsl

import akka.actor.ActorRef
import csw.logging.client.commons.LoggingKeys
import csw.logging.client.components.IRIS._
import csw.logging.client.components._
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.api.models.LoggingLevels._
import csw.logging.client.utils.LoggingTestSuite
import play.api.libs.json.JsObject

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

// DEOPSCSW-316: Improve Logger accessibility for component developers
class LoggerCompTest extends LoggingTestSuite {

  private val irisSupervisorActorRef = actorSystem.actorOf(IRIS.props(IRIS.COMPONENT_NAME))
  private val irisUtilActorRef       = actorSystem.actorOf(IrisActorUtil.props)
  private val irisTLA                = new IrisTLA()
  private val irisUtil               = new IrisUtil()
  private val tromboneHcd            = new TromboneHcd()

  private var componentLogBuffer: mutable.Map[String, ArrayBuffer[JsObject]] = mutable.Map.empty
  var genericLogBuffer: mutable.Buffer[JsObject]                             = mutable.Buffer.empty[JsObject]
  private var irisLogBuffer                                                  = mutable.Buffer.empty[JsObject]
  private var tromboneHcdLogBuffer                                           = mutable.Buffer.empty[JsObject]

  def sendMessagesToActor(actorRef: ActorRef): Unit = {
    actorRef ! LogTrace
    actorRef ! LogDebug
    actorRef ! LogInfo
    actorRef ! LogWarn
    actorRef ! LogError
    actorRef ! LogFatal
  }

  def allComponentsStartLogging(): Unit = {
    //componentName = IRIS
    sendMessagesToActor(irisSupervisorActorRef)
    irisTLA.startLogging()
    //Generic Logger
    sendMessagesToActor(irisUtilActorRef)
    irisUtil.startLogging(logMsgMap)
    //componentName = tromboneHcd
    tromboneHcd.startLogging(logMsgMap)
    Thread.sleep(300)
  }

  def splitAndGroupLogs(): Unit = {
    // clear all logs
    componentLogBuffer = mutable.Map.empty
    irisLogBuffer.clear()
    genericLogBuffer.clear()
    tromboneHcdLogBuffer.clear()

    logBuffer.foreach { log ⇒
      log.value.get(LoggingKeys.COMPONENT_NAME) match {
        case Some(_) ⇒
          val name = log.getString(LoggingKeys.COMPONENT_NAME)
          componentLogBuffer.get(name) match {
            case Some(xs) ⇒ componentLogBuffer.update(name, xs :+ log)
            case None     ⇒ componentLogBuffer.put(name, ArrayBuffer(log))
          }
        case None ⇒ genericLogBuffer += log
      }
    }
    irisLogBuffer = componentLogBuffer(IRIS.COMPONENT_NAME)
    tromboneHcdLogBuffer = componentLogBuffer(TromboneHcd.COMPONENT_NAME)

    logBuffer.clear()
  }

  // This test simulates single jvm multiple components use cases
  // DEOPSCSW-117: Provide unique name for each logging instance of components
  // DEOPSCSW-127: Runtime update for logging characteristics
  test("changing log level of component should only affect component specific classes") {
    allComponentsStartLogging()

    // extract component and non-component logs and group them
    splitAndGroupLogs()

    def testLogBuffer(logBuffer: mutable.Buffer[JsObject],
                      configuredLogLevel: Level,
                      expectedLogsMap: Map[String, String] = Map.empty,
                      expectedFileName: String = "",
                      expectedCompName: String = ""): Unit = {
      logBuffer.foreach { log ⇒
        val currentLogLevel = log.getString(LoggingKeys.SEVERITY).toLowerCase
        Level(currentLogLevel) >= configuredLogLevel shouldBe true
        if (expectedLogsMap.nonEmpty) log.getString(LoggingKeys.MESSAGE) shouldBe expectedLogsMap(currentLogLevel)
        if (!expectedFileName.isEmpty) log.getString(LoggingKeys.FILE) shouldBe expectedFileName
      }
    }

    irisLogBuffer.size shouldBe 4
    testLogBuffer(irisLogBuffer, ERROR, IRIS.irisLogs, IRIS.FILE_NAME, IRIS.COMPONENT_NAME)

    genericLogBuffer.size shouldBe 12
    testLogBuffer(genericLogBuffer, TRACE)

    tromboneHcdLogBuffer.size shouldBe 5
    testLogBuffer(tromboneHcdLogBuffer, DEBUG, logMsgMap, TromboneHcd.FILE_NAME, TromboneHcd.COMPONENT_NAME)

    // setting log level of IRIS comp to FATAL and it should not change log levels of other comps or generic classes
    loggingSystem.setComponentLogLevel(IRIS.COMPONENT_NAME, FATAL)

    // start logging at all component levels
    allComponentsStartLogging()

    // extract component and non-component logs and group them
    splitAndGroupLogs()

    irisLogBuffer.size shouldBe 2
    testLogBuffer(irisLogBuffer, FATAL, IRIS.irisLogs, IRIS.FILE_NAME, IRIS.COMPONENT_NAME)

    genericLogBuffer.size shouldBe 12
    testLogBuffer(genericLogBuffer, TRACE)

    tromboneHcdLogBuffer.size shouldBe 5
    testLogBuffer(tromboneHcdLogBuffer, DEBUG, logMsgMap, TromboneHcd.FILE_NAME, TromboneHcd.COMPONENT_NAME)
  }
}
