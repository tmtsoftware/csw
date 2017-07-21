package csw.services.logging

sealed trait LogCommand
object LogCommand {
  case object LogTrace extends LogCommand
  case object LogDebug extends LogCommand
  case object LogInfo extends LogCommand
  case object LogWarn extends LogCommand
  case object LogError extends LogCommand
  case object LogFatal extends LogCommand
  case object Unknown extends LogCommand
}

