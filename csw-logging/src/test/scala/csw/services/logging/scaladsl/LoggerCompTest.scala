package csw.services.logging.scaladsl

import akka.actor.ActorRef
import com.persist.JsonOps.JsonObject
import csw.services.logging.commons.LoggingKeys
import csw.services.logging.components.IRIS._
import csw.services.logging.components._
import csw.services.logging.internal.LoggingLevels._
import csw.services.logging.utils.LoggingTestSuite

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

// DEOPSCSW-316: Improve Logger accessibility for component developers
class LoggerCompTest extends LoggingTestSuite {

  private val irisSupervisorActorRef = actorSystem.actorOf(IRIS.props(IRIS.COMPONENT_NAME))
  private val irisUtilActorRef       = actorSystem.actorOf(IrisActorUtil.props)
  private val irisTLA                = new IrisTLA()
  private val irisUtil               = new IrisUtil()
  private val tromboneHcd            = new TromboneHcd()

  private var componentLogBuffer: mutable.Map[String, ArrayBuffer[JsonObject]] = mutable.Map.empty
  var genericLogBuffer: mutable.Buffer[JsonObject]                             = mutable.Buffer.empty[JsonObject]
  private var irisLogBuffer                                                    = mutable.Buffer.empty[JsonObject]
  private var tromboneHcdLogBuffer                                             = mutable.Buffer.empty[JsonObject]

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
    Thread.sleep(200)
  }

  def splitAndGroupLogs(): Unit = {
    // clear all logs
    componentLogBuffer = mutable.Map.empty
    irisLogBuffer.clear()
    genericLogBuffer.clear()
    tromboneHcdLogBuffer.clear()

    logBuffer.foreach { log ⇒
      log.get(LoggingKeys.COMPONENT_NAME) match {
        case Some(_) ⇒
          val name = log(LoggingKeys.COMPONENT_NAME).toString
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

    def testLogBuffer(logBuffer: mutable.Buffer[JsonObject],
                      configuredLogLevel: Level,
                      expectedLogsMap: Map[String, String] = Map.empty,
                      expectedFileName: String = "",
                      expectedCompName: String = ""): Unit = {
      logBuffer.foreach { log ⇒
        val currentLogLevel = log(LoggingKeys.SEVERITY).toString.toLowerCase
        Level(currentLogLevel) >= configuredLogLevel shouldBe true
        if (expectedLogsMap.nonEmpty) log(LoggingKeys.MESSAGE).toString shouldBe expectedLogsMap(currentLogLevel)
        if (!expectedFileName.isEmpty) log(LoggingKeys.FILE).toString shouldBe expectedFileName
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
