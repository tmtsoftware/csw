package csw.logging.client.appenders

import akka.actor.{ActorContext, ActorRefFactory, ActorSystem}
import csw.logging.api.models.LoggingLevels.Level
import csw.logging.client.commons.{Category, LoggingKeys}
import csw.logging.client.internal.JsonExtensions.RichJsObject
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Future

/**
 * Companion object for the StdOutAppender class
 */
object StdOutAppender extends LogAppenderBuilder {

  /**
   * A constructor for the StdOutAppender class
   *
   * @param factory an Akka factory
   * @param stdHeaders the headers that are fixes for this service
   * @return the stdout appender
   */
  def apply(factory: ActorRefFactory, stdHeaders: JsObject): StdOutAppender =
    new StdOutAppender(factory, stdHeaders, println)
}

/**
 * A log appender that writes common log messages to stdout. Stdout output can be printed as oneLine or pretty.
 * oneLine will print only the message of the log statement in single line and pretty will print all the information of log statement.
 *
 * @param factory an Akka factory
 * @param stdHeaders the headers that are fixes for this service
 */
class StdOutAppender(factory: ActorRefFactory, stdHeaders: JsObject, logPrinter: Any ⇒ Unit) extends LogAppender {
  private[this] val system = factory match {
    case context: ActorContext => context.system
    case s: ActorSystem        => s
  }
  private[this] val config        = system.settings.config.getConfig("csw-logging.appender-config.stdout")
  private[this] val fullHeaders   = config.getBoolean("fullHeaders")
  private[this] val color         = config.getBoolean("color")
  private[this] val summary       = config.getBoolean("summary")
  private[this] val pretty        = config.getBoolean("pretty")
  private[this] val oneLine       = config.getBoolean("oneLine")
  private[this] val logLevelLimit = Level(config.getString("logLevelLimit"))

  private[this] var categories = Map.empty[String, Int]
  private[this] var levels     = Map.empty[String, Int]
  private[this] var kinds      = Map.empty[String, Int]

  /**
   * Writes a log message to stdout
   *
   * @param baseMsg the message to be logged
   * @param category the kinds of log (for example, "common")
   */
  def append(baseMsg: JsObject, category: String): Unit = {
    val level = baseMsg.getString(LoggingKeys.SEVERITY)

    if (category == Category.Common.name && Level(level) >= logLevelLimit) {
      val maybeKind = baseMsg.getString(LoggingKeys.KIND)
      if (summary) {
        buildSummary(level, maybeKind)
      }
      val msg = if (fullHeaders) stdHeaders ++ baseMsg else baseMsg
      val normalText = if (oneLine) {
        oneLine(baseMsg, level, maybeKind)
      } else if (pretty) {
        Json.prettyPrint(msg - LoggingKeys.CATEGORY)
      } else {
        (msg - LoggingKeys.CATEGORY).toString()
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

  private def oneLine(baseMsg: JsObject, level: String, maybeKind: String) = {
    val msg       = baseMsg.getString(LoggingKeys.MESSAGE)
    val kind      = if (!maybeKind.isEmpty) s":$maybeKind" else ""
    val file      = baseMsg.getString(LoggingKeys.FILE)
    val where     = if (!file.isEmpty) s" ($file ${baseMsg.getString(LoggingKeys.LINE)})" else ""
    val comp      = baseMsg.getString(LoggingKeys.COMPONENT_NAME)
    val timestamp = baseMsg.getString(LoggingKeys.TIMESTAMP)

    val plainStack =
      if (baseMsg.contains(LoggingKeys.PLAINSTACK)) " [Stacktrace] " ++ baseMsg.getString(LoggingKeys.PLAINSTACK) else ""

    f"$timestamp $level%-5s$kind $comp$where - $msg$plainStack"
  }

  private def buildSummary(level: String, kind: String): Unit = {
    val levelCount = levels.getOrElse(level, 0) + 1
    levels += (level -> levelCount)
    if (!kind.isEmpty) {
      val cnt = kinds.getOrElse(kind, 0) + 1
      kinds += (kind -> cnt)
    }
  }

  /**
   * Called just before the logger shuts down
   *
   * @return a future that is completed when finished
   */
  def finish(): Future[Unit] =
    Future.successful(())

  /**
   * Closes the stdout appender
   *
   * @return a future that is completed when the close is complete
   */
  def stop(): Future[Unit] = {
    if (summary) {
      val cats = if (categories.isEmpty) Json.obj() else Json.obj("alts" -> categories)
      val levs = if (levels.isEmpty) Json.obj() else Json.obj("levels" -> levels)
      val knds = if (kinds.isEmpty) Json.obj() else Json.obj("kinds" -> kinds)
      val txt  = Json.prettyPrint(levs ++ cats ++ knds)
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
