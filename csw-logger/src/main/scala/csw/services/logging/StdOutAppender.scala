package csw.services.logging

import akka.actor.{ActorContext, ActorRefFactory, ActorSystem}
import com.persist.JsonOps._
import csw.services.logging.LoggingLevels.Level

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
  def apply(factory: ActorRefFactory, stdHeaders: Map[String, RichMsg]) = new StdOutAppender(factory, stdHeaders)
}

/**
 * A log appender that write common log messages to stdout.
 *
 * @param factory    an Akka factory.
 * @param stdHeaders the headers that are fixes for this service.
 */
class StdOutAppender(factory: ActorRefFactory, stdHeaders: Map[String, RichMsg]) extends LogAppender {
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
      if (summary) {
        val cnt = levels.get(level).getOrElse(0) + 1
        levels += (level -> cnt)
        val kind = jgetString(baseMsg, "kind")
        if (kind != "") {
          val cnt = kinds.get(kind).getOrElse(0) + 1
          kinds += (kind -> cnt)
        }
      }
      val msg = if (fullHeaders) stdHeaders ++ baseMsg else baseMsg
      val txt = if (oneLine) {
        val msg0 = jget(baseMsg, "msg")
        val msg = msg0 match {
          case s: String => s
          case x: Any    => Compact(x, safe = true)
        }
        val kind0 = jgetString(baseMsg, "kind")
        val kind  = if (kind0 == "") "" else s":$kind0"
        val file  = jgetString(baseMsg, "file")
        val line  = jgetInt(baseMsg, "line")
        val where = if (file != "") s" ($file $line)" else ""
        s"[$level$kind] $msg$where"
      } else if (pretty) {
        Pretty(msg - "@category", safe = true, width = width)
      } else {
        Compact(msg - "@category", safe = true)
      }

      val colorTxt = if (color) {
        level match {
          case "FATAL" | "ERROR" =>
            s"${Console.RED}$txt${Console.RESET}"
          case "WARN" =>
            s"${Console.YELLOW}$txt${Console.RESET}"
          case _ => txt
        }
      } else {
        txt
      }
      println(colorTxt)
    } else if (summary) {
      val cnt = categories.get(category).getOrElse(0) + 1
      categories += (category -> cnt)
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
      val cats = if (categories.size == 0) emptyJsonObject else JsonObject("alts" -> categories)
      val levs = if (levels.size == 0) emptyJsonObject else JsonObject("levels" -> levels)
      val knds = if (kinds.size == 0) emptyJsonObject else JsonObject("kinds" -> kinds)
      val txt  = Pretty(levs ++ cats ++ knds, width = width)
      val colorTxt = if (color) {
        s"${Console.BLUE}$txt${Console.RESET}"
      } else {
        txt
      }
      println(colorTxt)
    }
    Future.successful(())
  }
}
