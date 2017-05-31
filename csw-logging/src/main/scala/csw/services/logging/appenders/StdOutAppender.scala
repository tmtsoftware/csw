package csw.services.logging.appenders

import akka.actor.{ActorContext, ActorRefFactory, ActorSystem}
import com.persist.JsonOps._
import csw.services.logging.RichMsg
import csw.services.logging.internal.LoggingLevels.Level

import scala.concurrent.Future

/**
 * Companion object for the StdOutAppender class.
 */
object StdOutAppender extends LogAppenderBuilder {

  /**
   * A constructor for the StdOutAppender class.
   *
   * @param factory    an Akka factory.
   * @param stdHeaders the headers that are fixes for this service.
   * @return the stdout appender.
   */
  def apply(factory: ActorRefFactory, stdHeaders: Map[String, RichMsg]): StdOutAppender =
    new StdOutAppender(factory, stdHeaders, println)
}

/**
 * A log appender that write common log messages to stdout.
 *
 * @param factory    an Akka factory.
 * @param stdHeaders the headers that are fixes for this service.
 */
class StdOutAppender(factory: ActorRefFactory, stdHeaders: Map[String, RichMsg], logPrinter: Any ⇒ Unit)
    extends LogAppender {
  private[this] val system = factory match {
    case context: ActorContext => context.system
    case s: ActorSystem        => s
  }
  private[this] val config        = system.settings.config.getConfig("com.persist.logging.appenders.stdout")
  private[this] val fullHeaders   = config.getBoolean("fullHeaders")
  private[this] val color         = config.getBoolean("color")
  private[this] val width         = config.getInt("width")
  private[this] val summary       = config.getBoolean("summary")
  private[this] val pretty        = config.getBoolean("pretty")
  private[this] val oneLine       = config.getBoolean("oneLine")
  private[this] val logLevelLimit = Level(config.getString("logLevelLimit"))

  private[this] var categories = Map.empty[String, Int]
  private[this] var levels     = Map.empty[String, Int]
  private[this] var kinds      = Map.empty[String, Int]

  /**
   * Writes a log message to stdout.
   *
   * @param baseMsg  the message to be logged.
   * @param category the kinds of log (for example, "common").
   */
  def append(baseMsg: Map[String, RichMsg], category: String): Unit = {
    val level = jgetString(baseMsg, "@severity")

    if (category == "common" && Level(level) >= logLevelLimit) {
      val maybeKind = jgetString(baseMsg, "kind")
      if (summary) {
        buildSummary(maybeKind, level)
      }
      val msg = if (fullHeaders) stdHeaders ++ baseMsg else baseMsg
      val normalText = if (oneLine) {
        oneLine(baseMsg, level, maybeKind)
      } else if (pretty) {
        Pretty(msg - "@category", safe = true, width = width)
      } else {
        Compact(msg - "@category", safe = true)
      }

      val finalText = if (color) {
        coloredText(level, normalText)
      } else {
        normalText
      }
      logPrinter(finalText)

    } else if (summary) {
      val categoryCount = categories.getOrElse(category, 0) + 1
      categories += (category -> categoryCount)
    }
  }

  private def coloredText(level: String, normalText: String) =
    level match {
      case "FATAL" | "ERROR" =>
        s"${Console.RED}$normalText${Console.RESET}"
      case "WARN" =>
        s"${Console.YELLOW}$normalText${Console.RESET}"
      case _ => normalText
    }

  private def oneLine(baseMsg: Map[String, RichMsg], level: String, maybeKind: String) = {
    val msg = jget(baseMsg, "msg") match {
      case s: String => s
      case x: Any    => Compact(x, safe = true)
    }
    val kind  = if (!maybeKind.isEmpty) s":$maybeKind" else ""
    val file  = jgetString(baseMsg, "file")
    val where = if (!file.isEmpty) s" ($file ${jgetInt(baseMsg, "line")})" else ""
    s"[$level$kind] $msg$where"

  }

  private def buildSummary(level: String, kind: String) = {
    val levelCount = levels.getOrElse(level, 0) + 1
    levels += (level -> levelCount)
    if (!kind.isEmpty) {
      val cnt = kinds.getOrElse(kind, 0) + 1
      kinds += (kind -> cnt)
    }
  }

  /**
   * Called just before the logger shuts down.
   *
   * @return a future that is completed when finished.
   */
  def finish(): Future[Unit] =
    Future.successful(())

  /**
   * Closes the stdout appender.
   *
   * @return a future that is completed when the close is complete.
   */
  def stop(): Future[Unit] = {
    if (summary) {
      val cats = if (categories.isEmpty) emptyJsonObject else JsonObject("alts" -> categories)
      val levs = if (levels.isEmpty) emptyJsonObject else JsonObject("levels" -> levels)
      val knds = if (kinds.isEmpty) emptyJsonObject else JsonObject("kinds" -> kinds)
      val txt  = Pretty(levs ++ cats ++ knds, width = width)
      val colorTxt = if (color) {
        s"${Console.BLUE}$txt${Console.RESET}"
      } else {
        txt
      }
      logPrinter(colorTxt)
    }
    Future.successful(())
  }
}
