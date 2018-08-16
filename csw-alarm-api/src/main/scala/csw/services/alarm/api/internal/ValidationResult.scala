package csw.services.alarm.api.internal

private[alarm] sealed trait ValidationResult

private[alarm] object ValidationResult {
  case object Success                       extends ValidationResult
  case class Failure(reasons: List[String]) extends ValidationResult
}
