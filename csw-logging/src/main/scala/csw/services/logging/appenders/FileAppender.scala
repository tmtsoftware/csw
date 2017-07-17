package csw.services.logging.appenders

import java.io.{BufferedOutputStream, File, FileOutputStream, PrintWriter}
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}

import akka.actor._
import com.persist.JsonOps._
import csw.services.logging.RichMsg
import csw.services.logging.commons.{Category, Constants, LoggingKeys, TMTDateTimeFormatter}
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.macros.DefaultSourceLocation
import csw.services.logging.scaladsl.{Logger, LoggerImpl}

import scala.concurrent.{Future, Promise}

/**
 * Companion object of FileAppender Actor
 */
private[logging] object FileAppenderActor {

  // Parent trait for messages handles by File Appender Actor
  trait AppendMessages

  // Message to add log text to the writer
  case class AppendAdd(logDateTime: ZonedDateTime, line: String) extends AppendMessages

  // Message to close the appender
  case class AppendClose(p: Promise[Unit]) extends AppendMessages

  // Message to flush content from writer to the file
  case object AppendFlush extends AppendMessages

  def props(path: String, category: String): FileAppenderActor =
    new FileAppenderActor(path, category)
}

/**
 * Actor responsible for writing log messages to a file
 * @param path path where the log file will be created
 * @param category category of the log messages
 */
private[logging] class FileAppenderActor(path: String, category: String) {

  import FileAppenderActor._

  private[this] var fileSpanTimestamp: Option[ZonedDateTime] = None
  private[this] var maybePrintWriter: Option[PrintWriter]    = None

  private[this] var flushTimer: Option[Cancellable] = None
  protected val log: Logger                         = new LoggerImpl(None, None)

  // Initialize writer for log file
  private def open(logDateTime: ZonedDateTime): Unit = {
    val fileTimestamp = FileAppender.decideTimestampForFile(logDateTime)
    val dir           = s"$path"
    new File(dir).mkdirs()
    val fileName    = s"$dir/$category.$fileTimestamp.log"
    val printWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(fileName, true)))
    maybePrintWriter = Some(printWriter)
    fileSpanTimestamp = Some(fileTimestamp.plusDays(1L))
  }

  def !(msg: AppendMessages) = msg match {
    case AppendAdd(logDateTime, line) =>
      maybePrintWriter match {
        case Some(w) =>
          if (logDateTime
                .isAfter(fileSpanTimestamp.getOrElse(ZonedDateTime
                      .of(LocalDateTime.MIN, ZoneId.from(ZoneOffset.UTC))))) {
            w.close()
            open(logDateTime)
          }
        case None =>
          open(logDateTime)
      }
      maybePrintWriter match {
        case Some(w) =>
          w.println(line)
          w.flush()
        case None =>
      }

    case AppendClose(p) =>
      flushTimer match {
        case Some(t) => t.cancel()
        case None    =>
      }
      maybePrintWriter match {
        case Some(w) =>
          w.close()
          maybePrintWriter = None
        case None =>
      }
      p.success(())
      postStop
    case x: Any =>
      log.warn(s"Bad appender message: $x")(() => DefaultSourceLocation)
  }

  def postStop(): Unit =
    maybePrintWriter match {
      case Some(w) =>
        w.close()
        maybePrintWriter = None
      case None =>
    }
}

/**
 * Responsible for creating an actor which manages the file resource
 * @param actorRefFactory factory for creating an actor
 * @param path log file path
 * @param category log category
 */
private[logging] class FilesAppender(actorRefFactory: ActorRefFactory, path: String, category: String) {

  import FileAppenderActor._

  private[this] val fileAppenderActor = new FileAppenderActor(path, category)

  def add(logDateTime: ZonedDateTime, line: String): Unit =
    fileAppenderActor ! AppendAdd(logDateTime, line)

  def close(): Future[Unit] = {
    val p = Promise[Unit]()
    fileAppenderActor ! AppendClose(p)
    p.future
  }
}

/**
 * Companion object for FileAppender class.
 */
object FileAppender extends LogAppenderBuilder {

  /**
   * Constructor for a file appender.
   *
   * @param factory    an Akka factory.
   * @param stdHeaders the headers that are fixes for this service.
   * @return
   */
  def apply(factory: ActorRefFactory, stdHeaders: Map[String, RichMsg]): FileAppender =
    new FileAppender(factory, stdHeaders)

  def decideTimestampForFile(logDateTime: ZonedDateTime): ZonedDateTime = {
    val fileTimestamp =
      if (logDateTime.getHour >= Constants.FILE_ROTATION_HOUR)
        logDateTime
          .truncatedTo(ChronoUnit.DAYS)
          .plusHours(Constants.FILE_ROTATION_HOUR)
      else
        logDateTime
          .truncatedTo(ChronoUnit.DAYS)
          .minusDays(1L)
          .plusHours(Constants.FILE_ROTATION_HOUR)
    fileTimestamp
  }
}

/**
 * An appender that writes log messages to files.
 *
 * @param factory ActorRefFactory
 * @param stdHeaders the headers that are fixes for this service.
 */
class FileAppender(factory: ActorRefFactory, stdHeaders: Map[String, RichMsg]) extends LogAppender {
  private[this] val system = factory match {
    case context: ActorContext => context.system
    case s: ActorSystem        => s
  }
  private[this] implicit val executionContext = factory.dispatcher
  private[this] val config =
    system.settings.config.getConfig("csw-logging.appender-config.file")
  private[this] val fullHeaders   = config.getBoolean("fullHeaders")
  private[this] val logPath       = config.getString("logPath")
  private[this] val sort          = config.getBoolean("sorted")
  private[this] val logLevelLimit = Level(config.getString("logLevelLimit"))
  private[this] val fileAppenders =
    scala.collection.mutable.HashMap[String, FilesAppender]()
  private val loggingSystemName = jgetString(stdHeaders, LoggingKeys.NAME)

  private def checkLevel(baseMsg: Map[String, RichMsg]): Boolean = {
    val level = jgetString(baseMsg, LoggingKeys.SEVERITY)
    Level(level) >= logLevelLimit
  }

  /**
   * Write the log message to a file.
   *
   * @param baseMsg  the message to be logged.
   * @param category the kinds of log (for example, "common").
   */
  def append(baseMsg: Map[String, RichMsg], category: String): Unit =
    if (category != Category.Common.name || checkLevel(baseMsg)) {
      val msg = if (fullHeaders) stdHeaders ++ baseMsg else baseMsg
      //Maintain a file appender for each category in a logging system
      val fileAppenderKey = loggingSystemName + "-" + category
      val fileAppender = fileAppenders.get(fileAppenderKey) match {
        case Some(appender) => appender
        case None           =>
          //Create a file appender with logging file directory as logging system name within the log file path
          val filesAppender = new FilesAppender(factory, logPath + "/" + loggingSystemName, category)
          fileAppenders += (fileAppenderKey -> filesAppender)
          filesAppender
      }
      val timestamp = jgetString(msg, LoggingKeys.TIMESTAMP)

      val logDateTime = TMTDateTimeFormatter.parse(timestamp)
      fileAppender.add(logDateTime, Compact(msg, safe = true, sort = sort))
    }

  /**
   * Called just before the logger shuts down.
   *
   * @return a future that is completed when finished.
   */
  def finish(): Future[Unit] =
    Future.successful(())

  /**
   * Closes the file appender.
   *
   * @return a future that is completed when the close is complete.
   */
  def stop(): Future[Unit] = {
    val fs = for ((category, appender) <- fileAppenders) yield {
      appender.close()
    }
    Future.sequence(fs).map(s => ())
  }
}
