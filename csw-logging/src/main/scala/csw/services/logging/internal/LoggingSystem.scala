package csw.services.logging.internal

import java.util.concurrent.CompletableFuture

import akka.Done
import akka.actor.{ActorSystem, Props}
import ch.qos.logback.classic.LoggerContext
import csw.services.logging.RichMsg
import csw.services.logging.appenders.LogAppenderBuilder
import csw.services.logging.internal.TimeActorMessages.TimeDone
import csw.services.logging.macros.DefaultSourceLocation
import csw.services.logging.models.LogMetadata
import csw.services.logging.scaladsl.GenericLogger
import org.slf4j.LoggerFactory

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * This class is responsible for programmatic interaction with the configuration of the logging system. It initializes
 * the appenders, starts the log actor and manages clean up of logging system.
 * @param name             name of the service (to log).
 * @param version             version of the service (to log).
 * @param host             host name (to log).
 * @param system           actor system which will be used to create log actors
 * @param appenderBuilders sequence of log appenders to use.
 *
 */
class LoggingSystem(name: String,
                    version: String,
                    host: String,
                    system: ActorSystem,
                    appenderBuilders: Seq[LogAppenderBuilder])
    extends GenericLogger.Simple {

  import LoggingLevels._

  private[this] val loggingConfig =
    system.settings.config.getConfig("csw-logging")

  private[this] val levels = loggingConfig.getString("logLevel")
  private[this] val defaultLevel: Level = if (Level.hasLevel(levels)) {
    Level(levels)
  } else {
    throw new Exception("Bad value for csw-logging.logLevel")
  }
  @volatile var logLevel: Level = defaultLevel

  private[this] val akkaLogLevelS = loggingConfig.getString("akkaLogLevel")
  private[this] val defaultAkkaLogLevel: Level =
    if (Level.hasLevel(akkaLogLevelS)) {
      Level(akkaLogLevelS)
    } else {
      throw new Exception("Bad value for csw-logging.akkaLogLevel")
    }
  @volatile private[this] var akkaLogLevel = defaultAkkaLogLevel

  private[this] val slf4jLogLevelS = loggingConfig.getString("slf4jLogLevel")
  private[this] val defaultSlf4jLogLevel: Level =
    if (Level.hasLevel(slf4jLogLevelS)) {
      Level(slf4jLogLevelS)
    } else {
      throw new Exception("Bad value for csw-logging.slf4jLogLevel")
    }
  @volatile private[this] var slf4jLogLevel = defaultSlf4jLogLevel

  private[this] val gc   = loggingConfig.getBoolean("gc")
  private[this] val time = loggingConfig.getBoolean("time")

  private[this] implicit val ec: ExecutionContext = system.dispatcher
  private[this] val done                          = Promise[Unit]
  private[this] val timeActorDonePromise          = Promise[Unit]

  private[this] val initialComponentsLoggingState = ComponentLoggingStateManager.from(loggingConfig)

  LoggingState.componentsLoggingState = LoggingState.componentsLoggingState ++ initialComponentsLoggingState

  /**
   * Standard headers.
   */
  val standardHeaders: Map[String, RichMsg] =
    Map[String, RichMsg]("@host" -> host, "@name" -> name, "@version" -> version)

  setDefaultLogLevel(defaultLevel)
  LoggingState.loggerStopping = false
  LoggingState.doTime = false
  LoggingState.timeActorOption = None

  private[this] val appenders = appenderBuilders.map {
    _.apply(system, standardHeaders)
  }

  private[this] val logActor = system.actorOf(LogActor.props(done, standardHeaders, appenders, defaultLevel,
      defaultSlf4jLogLevel, defaultAkkaLogLevel), name = "LoggingActor")
  LoggingState.maybeLogActor = Some(logActor)

  private[logging] val gcLogger: Option[GcLogger] = if (gc) {
    Some(new GcLogger)
  } else {
    None
  }

  if (time) {
    // Start timing actor
    LoggingState.doTime = true
    val timeActor = system.actorOf(Props(new TimeActor(timeActorDonePromise)), name = "TimingActor")
    LoggingState.timeActorOption = Some(timeActor)
  } else {
    timeActorDonePromise.success(())
  }

  // Deal with messages send before logger was ready
  LoggingState.msgs.synchronized {
    if (LoggingState.msgs.nonEmpty) {
      log.info(s"Saw ${LoggingState.msgs.size} messages before logger start")(() => DefaultSourceLocation)
      for (msg <- LoggingState.msgs) {
        MessageHandler.sendMsg(msg)
      }
    }
    LoggingState.msgs.clear()
  }

  /**
   * Get logging levels.
   * @return the current and default logging levels.
   */
  def getDefaultLogLevel: Levels = Levels(logLevel, defaultLevel)

  /**
   * Changes the logger API logging level.
   * @param level the new logging level for the logger API.
   */
  def setDefaultLogLevel(level: Level): Unit = {
    logLevel = level
    LoggingState.componentsLoggingState("default").setLevel(level)
  }

  /**
   * Get Akka logging levels
   * @return the current and default Akka logging levels.
   */
  def getAkkaLevel: Levels = Levels(akkaLogLevel, defaultAkkaLogLevel)

  /**
   * Changes the Akka logger logging level.
   * @param level the new logging level for the Akka logger.
   */
  def setAkkaLevel(level: Level): Unit = {
    akkaLogLevel = level
    logActor ! SetAkkaLevel(level)
  }

  /**
   * Get the Slf4j logging levels.
   * @return the current and default Slf4j logging levels.
   */
  def getSlf4jLevel: Levels = Levels(slf4jLogLevel, defaultSlf4jLogLevel)

  /**
   * Changes the slf4j logging level.
   * @param level the new logging level for slf4j.
   */
  def setSlf4jLevel(level: Level): Unit = {
    slf4jLogLevel = level
    logActor ! SetSlf4jLevel(level)
  }

  // todo: this can be achieved through LoggerImpl as well, decide the place for setting log level dynamically while playing framework stories
  def setComponentLogLevel(componentName: String, level: Level): Unit =
    ComponentLoggingStateManager.add(componentName, level)

  // todo: this can be achieved through LoggerImpl as well, decide the place for setting log level dynamically while playing framework stories
  /**
   * Get the basic logging configuration values
   * @return LogMetadata which comprises of current root log level, akka log level, sl4j log level and component log level
   */
  def getLogMetadata(componentName: String): LogMetadata =
    LogMetadata(
      getDefaultLogLevel.current,
      getAkkaLevel.current,
      getSlf4jLevel.current,
      LoggingState.componentsLoggingState.getOrElse(componentName, LoggingState.componentsLoggingState("default")).componentLogLevel
    )

  /**
   * Shut down the logging system.
   * @return  future completes when the logging system is shut down.
   */
  def stop: Future[Done] = {
    def stopAkka(): Future[Unit] = {
      MessageHandler.sendMsg(LastAkkaMessage)
      LoggingState.akkaStopPromise.future
    }

    def stopTimeActor(): Future[Unit] = {
      LoggingState.timeActorOption foreach (timeActor => timeActor ! TimeDone)
      timeActorDonePromise.future
    }

    def stopLogger(): Future[Unit] = {
      LoggingState.loggerStopping = true
      logActor ! StopLogging
      LoggingState.maybeLogActor = None
      done.future
    }

    def finishAppenders(): Future[Unit] =
      Future.sequence(appenders map (_.finish())).map(x => ())

    def stopAppenders(): Future[Unit] =
      Future.sequence(appenders map (_.stop())).map(x => ())

    //Stop gc logger
    gcLogger foreach (_.stop())

    // Stop Slf4j
    val loggerContext =
      LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    loggerContext.stop()

    for {
      akkaTimeDone  <- stopAkka() zip stopTimeActor()
      logActorDone  <- finishAppenders()
      logActorDone  <- stopLogger()
      appendersDone <- stopAppenders()
    } yield Done
  }

  def javaStop(): CompletableFuture[Done] = stop.toJava.toCompletableFuture
}
