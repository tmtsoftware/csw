package csw.services.logging

import ch.qos.logback.core.{Appender, UnsynchronizedAppenderBase}
import ch.qos.logback.core.spi.AppenderAttachable

import scala.collection.mutable

private[logging] class Slf4jAppender[E]()
    extends UnsynchronizedAppenderBase[E]
    with AppenderAttachable[E]
    with ClassLogging {
  import LoggingLevels._

  val appenders: mutable.HashSet[Appender[E]] = scala.collection.mutable.HashSet[Appender[E]]()

  def detachAndStopAllAppenders(): Unit = {}

  def detachAppender(x$1: String): Boolean = true

  def detachAppender(x$1: ch.qos.logback.core.Appender[E]): Boolean = true

  def getAppender(x$1: String): ch.qos.logback.core.Appender[E] = null

  def isAttached(x$1: ch.qos.logback.core.Appender[E]): Boolean = true

  def iteratorForAppenders(): java.util.Iterator[ch.qos.logback.core.Appender[E]] = null

  def addAppender(a: Appender[E]): Unit =
    appenders.add(a)

  def append(event: E): Unit =
    event match {
      case e: ch.qos.logback.classic.spi.LoggingEvent =>
        val frame = e.getCallerData()(0)
        val level = Level(e.getLevel.toString)
        val ex = try {
          val x = e.getThrowableProxy.asInstanceOf[ch.qos.logback.classic.spi.ThrowableProxy]
          x.getThrowable
        } catch {
          case ex: Any => noException
        }
        val msg = LogSlf4j(level, e.getTimeStamp, frame.getClassName, e.getFormattedMessage, frame.getLineNumber,
          frame.getFileName, ex)
        MessageHandler.sendMsg(msg)
      case x: Any =>
        log.warn(s"UNEXPECTED LOGBACK EVENT:${event.getClass}:$event")(() => DefaultSourceLocation)
    }
}
