package csw.services.logging.appenders

import java.io._
import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}

import akka.actor._
import com.persist.JsonOps._
import csw.services.logging.RichMsg
import csw.services.logging.commons.{Category, Constants, LoggingKeys, TMTDateTimeFormatter}
import csw.services.logging.internal.LoggingLevels.Level
import csw.services.logging.scaladsl.{Logger, LoggerImpl}

import scala.concurrent.{ExecutionContextExecutor, Future, Promise}

/**
 * Responsible for writing log messages to a file on local disk
 *
 * @param path     Path where the log file will be created
 * @param category Category of the log messages
 */
private[logging] class FileAppenderHelper(path: String, category: String) {

  private[this] var fileSpanTimestamp: Option[ZonedDateTime] = None
  private[this] var maybePrintWriter: Option[PrintWriter]    = None

  protected val log: Logger = new LoggerImpl(None, None)

  // The file containing logs is created on local machine. This file is rotated everyday at 12:00:00 hour.
  def appendAdd(maybeTimestamp: Option[ZonedDateTime], line: String, rotateFlag: Boolean): Unit = {
    maybePrintWriter match {
      case Some(w) =>
        if (rotateFlag && maybeTimestamp.get
              .isAfter(
                fileSpanTimestamp.getOrElse(
                  ZonedDateTime
                    .of(LocalDateTime.MIN, ZoneId.from(ZoneOffset.UTC))
                )
              )) {
          w.close()
          open(maybeTimestamp, rotateFlag)
        }
      case None =>
        open(maybeTimestamp, rotateFlag)
    }
    maybePrintWriter match {
      case Some(w) =>
        w.println(line)
        w.flush()
      case None =>
    }
  }

  def appendClose(p: Promise[Unit]): Unit = {
    maybePrintWriter match {
      case Some(w) =>
        w.close()
        maybePrintWriter = None
      case None =>
    }
    p.success(())
    postStop()
  }

  def postStop(): Unit =
    maybePrintWriter match {
      case Some(w) =>
        w.close()
        maybePrintWriter = None
      case None =>
    }

  // Initialize writer for log file
  private def open(maybeTimestamp: Option[ZonedDateTime], rotateFlag: Boolean): Unit = {
    val dir = s"$path"

    val fileName = if (rotateFlag) {
      val fileTimestamp = FileAppender.decideTimestampForFile(maybeTimestamp.get)
      fileSpanTimestamp = Some(fileTimestamp.plusDays(1L))
      s"$dir/$category.$fileTimestamp.log"
    } else {
      s"$dir/$category.log"
    }

    new File(dir).mkdirs()
    val printWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(fileName, true)))
    maybePrintWriter = Some(printWriter)
  }
}

/**
 * Responsible for creating an FileAppenderHelper which manages the file resource
 * @param path log file path
 * @param category log category
 */
private[logging] class FilesAppender(path: String, category: String) {

  private[this] val fileAppenderHelper = new FileAppenderHelper(path, category)

  def add(maybeTimestamp: Option[ZonedDateTime], line: String, rotateFlag: Boolean): Unit =
    fileAppenderHelper.appendAdd(maybeTimestamp, line, rotateFlag)

  def close(): Future[Unit] = {
    val p = Promise[Unit]()
    fileAppenderHelper.appendClose(p)
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
  private[this] implicit val executionContext: ExecutionContextExecutor = factory.dispatcher
  private[this] val config =
    system.settings.config.getConfig("csw-logging.appender-config.file")
  private[this] val fullHeaders   = config.getBoolean("fullHeaders")
  private[this] val logPath       = config.getString("logPath")
  private[this] val sort          = config.getBoolean("sorted")
  private[this] val logLevelLimit = Level(config.getString("logLevelLimit"))
  private[this] val rotateFlag    = config.getBoolean("rotate")
  private[this] val fileAppenders =
    scala.collection.mutable.HashMap[String, FilesAppender]()
  private val loggingSystemName = stdHeaders(LoggingKeys.NAME).toString

  private def checkLevel(baseMsg: Map[String, RichMsg]): Boolean = {
    val level = baseMsg(LoggingKeys.SEVERITY).toString
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
      val msg = (if (fullHeaders) stdHeaders ++ baseMsg else baseMsg) - LoggingKeys.PLAINSTACK
      // Maintain a file appender for each category in a logging system
      val fileAppenderKey = loggingSystemName + "-" + category
      val fileAppender = fileAppenders.get(fileAppenderKey) match {
        case Some(appender) => appender
        case None           =>
          // Create a file appender with logging file directory as logging system name within the log file path
          val filesAppender = new FilesAppender(logPath + "/" + loggingSystemName, category)
          fileAppenders += (fileAppenderKey -> filesAppender)
          filesAppender
      }
      val maybeTimestamp = if (rotateFlag) {
        val timestamp = baseMsg(LoggingKeys.TIMESTAMP).toString
        Some(TMTDateTimeFormatter.parse(timestamp))
      } else None

      fileAppender.add(maybeTimestamp, Compact(msg, safe = true, sort = sort), rotateFlag)
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
    Future.sequence(fs).map(_ => ())
  }
}
