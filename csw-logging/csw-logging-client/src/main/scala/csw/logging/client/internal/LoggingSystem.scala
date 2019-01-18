package csw.logging.client.internal

import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

import akka.Done
import akka.actor.{ActorSystem, Props}
import ch.qos.logback.classic.LoggerContext
import csw.logging.api.scaladsl.Logger
import csw.logging.client.appenders.LogAppenderBuilder
import csw.logging.client.commons.{Constants, LoggingKeys}
import csw.logging.client.exceptions.AppenderNotFoundException
import csw.logging.client.internal.LogActorMessages._
import csw.logging.client.internal.TimeActorMessages.TimeDone
import csw.logging.client.models.LogMetadata
import csw.logging.client.scaladsl.GenericLoggerFactory
import csw.logging.macros.DefaultSourceLocation
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * This class is responsible for programmatic interaction with the configuration of the logging system. It initializes
 * appenders, starts the log actor and manages clean up of logging system. Until and unless this class is instantiated
 * all(akka, slf4j and tmt) the logs are enqueued in local queue. Once it is instantiated, the queue is emptied and all
 * the logs are forwarded to configured appenders.
 *
 * @param name name of the service (to log).
 * @param version version of the service (to log).
 * @param host host name (to log).
 * @param system an ActorSystem used to create log actors
 */
private[csw] class LoggingSystem(name: String, version: String, host: String, val system: ActorSystem) {

  import csw.logging.api.models.LoggingLevels._

  private val log: Logger = GenericLoggerFactory.getLogger

  private[this] val loggingConfig = system.settings.config.getConfig("csw-logging")

  private[this] val defaultAppenderBuilders: List[LogAppenderBuilder] =
    loggingConfig.getStringList("appenders").asScala.toList.map(getAppenderInstance)

  @volatile var appenderBuilders: List[LogAppenderBuilder] = defaultAppenderBuilders

  private[this] val levels = loggingConfig.getString("logLevel")
  private[this] val defaultLevel: Level =
    if (Level.hasLevel(levels)) Level(levels)
    else throw new Exception(s"Bad value $levels for csw-logging.logLevel")

  LoggingState.logLevel = defaultLevel

  private[this] val akkaLogLevelS = loggingConfig.getString("akkaLogLevel")
  private[this] val defaultAkkaLogLevel: Level =
    if (Level.hasLevel(akkaLogLevelS)) Level(akkaLogLevelS)
    else throw new Exception(s"Bad value $akkaLogLevelS for csw-logging.akkaLogLevel")

  LoggingState.akkaLogLevel = defaultAkkaLogLevel

  private[this] val slf4jLogLevelS = loggingConfig.getString("slf4jLogLevel")
  private[this] val defaultSlf4jLogLevel: Level =
    if (Level.hasLevel(slf4jLogLevelS)) Level(slf4jLogLevelS)
    else throw new Exception(s"Bad value $slf4jLogLevelS for csw-logging.slf4jLogLevel")

  LoggingState.slf4jLogLevel = defaultSlf4jLogLevel

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
  val standardHeaders: JsObject = Json.obj(LoggingKeys.HOST -> host, LoggingKeys.NAME -> name, LoggingKeys.VERSION -> version)

  setDefaultLogLevel(defaultLevel)
  LoggingState.loggerStopping = false
  LoggingState.doTime = false
  LoggingState.timeActorOption = None

  @volatile private[this] var appenders = appenderBuilders.map {
    _.apply(system, standardHeaders)
  }

  private[this] val logActor = system.actorOf(
    LogActor.props(done, standardHeaders, appenders, defaultLevel, defaultSlf4jLogLevel, defaultAkkaLogLevel),
    name = "LoggingActor"
  )
  LoggingState.maybeLogActor = Some(logActor)

  private[logging] val gcLogger: Option[GcLogger] =
    if (gc) Some(new GcLogger)
    else None

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
      // DefaultSourceLocation will have empty `file`, `line` and `class`
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
  def getDefaultLogLevel: Levels = Levels(LoggingState.logLevel, defaultLevel)

  /**
   * Changes the logger API logging level.
   * @param level the new logging level for the logger API.
   */
  def setDefaultLogLevel(level: Level): Unit = {
    LoggingState.logLevel = level
    LoggingState.componentsLoggingState(Constants.DEFAULT_KEY).setLevel(level)
  }

  /**
   * Get Akka logging levels
   *
   * @return the current and default Akka logging levels.
   */
  def getAkkaLevel: Levels = Levels(LoggingState.akkaLogLevel, defaultAkkaLogLevel)

  /**
   * Changes the Akka logger logging level.
   * @param level the new logging level for the Akka logger.
   */
  def setAkkaLevel(level: Level): Unit = {
    LoggingState.akkaLogLevel = level
    logActor ! SetAkkaLevel(level)
  }

  /**
   * Get the Slf4j logging levels.
   *
   * @return the current and default Slf4j logging levels.
   */
  def getSlf4jLevel: Levels = Levels(LoggingState.slf4jLogLevel, defaultSlf4jLogLevel)

  /**
   * Changes the slf4j logging level.
   *
   * @param level the new logging level for slf4j.
   */
  def setSlf4jLevel(level: Level): Unit = {
    LoggingState.slf4jLogLevel = level
    logActor ! SetSlf4jLevel(level)
  }

  /**
   * Get the logging appenders.
   *
   * @return the current and default logging appenders.
   */
  def getAppenders: List[LogAppenderBuilder] = appenderBuilders

  /**
   * Changes the logging appenders.
   *
   * @param _appenderBuilders the list of new logging appenders.
   */
  def setAppenders(_appenderBuilders: List[LogAppenderBuilder]): Unit = {
    appenderBuilders = _appenderBuilders
    appenders = appenderBuilders.map {
      _.apply(system, standardHeaders)
    }
    logActor ! SetAppenders(appenders)
  }

  def setComponentLogLevel(componentName: String, level: Level): Unit =
    ComponentLoggingStateManager.add(componentName, level)

  /**
   * Get the basic logging configuration values
   *
   * @return LogMetadata which comprises of current root log level, akka log level, sl4j log level and component log level
   */
  def getLogMetadata(componentName: String): LogMetadata =
    LogMetadata(
      getDefaultLogLevel.current,
      getAkkaLevel.current,
      getSlf4jLevel.current,
      LoggingState.componentsLoggingState
        .getOrElse(componentName, LoggingState.componentsLoggingState(Constants.DEFAULT_KEY))
        .componentLogLevel
    )

  /**
   * Shut down the logging system.
   *
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

    def finishAppenders(): Future[Unit] = Future.sequence(appenders map (_.finish())).map(_ => ())
    def stopAppenders(): Future[Unit]   = Future.sequence(appenders map (_.stop())).map(_ => ())

    //Stop gc logger
    gcLogger.foreach(_.stop())

    // Stop Slf4j
    val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    loggerContext.stop()

    for {
      _ <- stopAkka() zip stopTimeActor()
      _ <- finishAppenders()
      _ <- stopLogger()
      _ <- stopAppenders()
    } yield Done
  }

  def javaStop(): CompletableFuture[Done] = stop.toJava.toCompletableFuture

  private def getAppenderInstance(appender: String): LogAppenderBuilder = {
    try {
      if (appender.endsWith("$"))
        Class.forName(appender).getField("MODULE$").get(null).asInstanceOf[LogAppenderBuilder]
      else {
        val buf = ByteBuffer.allocateDirect(1)
        try {
          val directByteBufferConstr = buf.getClass.getDeclaredConstructor(classOf[Long], classOf[Int], classOf[Any])
          directByteBufferConstr.setAccessible(true)
        } catch {
          case e: Exception ⇒
        }
        Class.forName(appender).getDeclaredConstructor().newInstance().asInstanceOf[LogAppenderBuilder]
      }
    } catch {
      case _: Throwable ⇒ throw AppenderNotFoundException(appender)
    }
  }
}
