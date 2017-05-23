package csw.services.logging

import java.io.{BufferedOutputStream, File, FileOutputStream, PrintWriter}

import akka.actor._
import com.persist.JsonOps._
import csw.services.logging.LoggingLevels.Level

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

private[logging] object FileAppenderActor {

  trait AppendMessages

  case class AppendAdd(data: String, line: String) extends AppendMessages

  case class AppendClose(p: Promise[Unit]) extends AppendMessages

  case object AppendFlush extends AppendMessages

  def props(path: String, category: String): Props = Props(new FileAppenderActor(path, category))
}

private[logging] class FileAppenderActor(path: String, category: String) extends GenericLogger.Actor {

  import FileAppenderActor._

  private[this] val system                        = context.system
  private[this] implicit val ec: ExecutionContext = context.dispatcher
  private[this] var lastDate: String              = ""
  private[this] var optw: Option[PrintWriter]     = None

  private[this] var flushTimer: Option[Cancellable] = None

  private def scheduleFlush(): Unit = {
    val time = system.scheduler.scheduleOnce(2.seconds) {
      self ! AppendFlush
    }
    flushTimer = Some(time)
  }

  scheduleFlush()

  private def open(date: String): Unit = {
    val dir = s"$path"
    new File(dir).mkdirs()
    val fname = s"$dir/$category.$date.log"
    val w     = new PrintWriter(new BufferedOutputStream(new FileOutputStream(fname, true)))
    optw = Some(w)
    lastDate = date
  }

  def receive: PartialFunction[Any, Unit] = {
    case AppendAdd(date, line) =>
      optw match {
        case Some(w) =>
          if (date != lastDate) {
            w.close()
            open(date)
          }
        case None =>
          open(date)
      }
      optw match {
        case Some(w) =>
          w.println(line)
        case None =>
      }
    case AppendFlush =>
      optw match {
        case Some(w) =>
          w.flush()
        case None =>
      }
      scheduleFlush()

    case AppendClose(p) =>
      flushTimer match {
        case Some(t) => t.cancel()
        case None    =>
      }
      optw match {
        case Some(w) =>
          w.close()
          optw = None
        case None =>
      }
      p.success(())
      context.stop(self)
    case x: Any => log.warn(s"Bad appender message: $x")(() => DefaultSourceLocation)
  }

  override def postStop(): Unit =
    optw match {
      case Some(w) =>
        w.close()
        optw = None
      case None =>
    }
}

private[logging] case class FilesAppender(actorRefFactory: ActorRefFactory, path: String, category: String) {

  import FileAppenderActor._

  private[this] val fileAppenderActor =
    actorRefFactory.actorOf(FileAppenderActor.props(path, category), name = s"FileAppender.$category")

  def add(date: String, line: String): Unit =
    fileAppenderActor ! AppendAdd(date, line)

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
  private[this] val config                    = system.settings.config.getConfig("com.persist.logging.appenders.file")
  private[this] val fullHeaders               = config.getBoolean("fullHeaders")
  private[this] val logPath                   = config.getString("logPath")
  private[this] val sort                      = config.getBoolean("sorted")
  private[this] val serviceInPath             = config.getBoolean("serviceInPath")
  private[this] val logLevelLimit             = Level(config.getString("logLevelLimit"))
  private[this] val fileAppenders             = scala.collection.mutable.HashMap[String, FilesAppender]()
  private[this] val fullPath: String = if (serviceInPath) {
    logPath + "/" + jgetString(stdHeaders, "@service", "name")
  } else {
    logPath
  }

  private def checkLevel(baseMsg: Map[String, RichMsg]): Boolean = {
    val level = jgetString(baseMsg, "@severity")
    Level(level) >= logLevelLimit
  }

  /**
   * Write the log message to a file.
   *
   * @param baseMsg  the message to be logged.
   * @param category the kinds of log (for example, "common").
   */
  def append(baseMsg: Map[String, RichMsg], category: String): Unit =
    if (category != "common" || checkLevel(baseMsg)) {
      val msg = if (fullHeaders) stdHeaders ++ baseMsg else baseMsg
      val fa = fileAppenders.get(category) match {
        case Some(a) => a
        case None =>
          val a = FilesAppender(factory, fullPath, category)
          fileAppenders += (category -> a)
          a
      }
      val date = jgetString(msg, "@timestamp").substring(0, 10)
      fa.add(date, Compact(msg, safe = true, sort = sort))
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
