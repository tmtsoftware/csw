package csw.logging.core.models

import csw.logging.core.internal.LoggingLevels._

/**
 * A LoggingState maintained for each component
 *
 * @param level the log level applicable for a component
 */
private[logging] class ComponentLoggingState(level: Level) {

  private[logging] var componentLogLevel: Level = level

  @volatile var doTrace: Boolean = false
  @volatile var doDebug: Boolean = false
  @volatile var doInfo: Boolean  = true
  @volatile var doWarn: Boolean  = true
  @volatile var doError: Boolean = true
  @volatile var doFatal: Boolean = true

  def setLevel(level: Level): Unit = {
    componentLogLevel = level
    doTrace = level.pos <= TRACE.pos
    doDebug = level.pos <= DEBUG.pos
    doInfo = level.pos <= INFO.pos
    doWarn = level.pos <= WARN.pos
    doError = level.pos <= ERROR.pos
    doFatal = level.pos <= FATAL.pos
  }

  override def toString(): String = componentLogLevel.toString
}

private[logging] object ComponentLoggingState {
  def apply(level: Level): ComponentLoggingState = {
    val componentLoggingState = new ComponentLoggingState(level)
    componentLoggingState.setLevel(level)
    componentLoggingState
  }
}
