package csw.logging.compat

import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.spi.AppenderAttachable
import ch.qos.logback.core.{Appender, UnsynchronizedAppenderBase}
import csw.logging.internal.LogActorMessages.LogSlf4j
import csw.logging.internal.MessageHandler
import csw.logging.macros.DefaultSourceLocation
import csw.logging.NoLogException
import csw.logging.scaladsl.{GenericLoggerFactory, Logger}

import scala.collection.mutable

/**
 * This class is wired up as appender in `logback.xml`. The instance of this class is created via reflection. When log
 * statement from SLF4J code is executed, a message is sent to this class. Then this class will simply process the received message
 * and forward it to underlying logging code.
 */
private[logging] class Slf4jAppender[E]() extends UnsynchronizedAppenderBase[E] with AppenderAttachable[E] {
  import csw.logging.internal.LoggingLevels._

  private val log: Logger = GenericLoggerFactory.getLogger

  private val appenders: mutable.HashSet[Appender[E]] = scala.collection.mutable.HashSet[Appender[E]]()

  def detachAndStopAllAppenders(): Unit = {}

  def detachAppender(x$1: String): Boolean = true

  def detachAppender(x$1: ch.qos.logback.core.Appender[E]): Boolean = true

  def getAppender(x$1: String): ch.qos.logback.core.Appender[E] = null

  def isAttached(x$1: ch.qos.logback.core.Appender[E]): Boolean = true

  def iteratorForAppenders(): java.util.Iterator[ch.qos.logback.core.Appender[E]] = null

  def addAppender(a: Appender[E]): Unit = appenders.add(a)

  def append(event: E): Unit =
    event match {
      case e: ch.qos.logback.classic.spi.LoggingEvent =>
        val frame = e.getCallerData()(0)
        val level = Level(e.getLevel.toString)
        val ex = try {
          e.getThrowableProxy.asInstanceOf[ThrowableProxy].getThrowable
        } catch {
          case ex: Any => NoLogException
        }
        val msg =
          LogSlf4j(level, e.getTimeStamp, frame.getClassName, e.getFormattedMessage, frame.getLineNumber, frame.getFileName, ex)
        MessageHandler.sendMsg(msg)
      case x: Any =>
        log.warn(s"UNEXPECTED LOGBACK EVENT:${event.getClass}:$event")(() => DefaultSourceLocation)
    }
}
