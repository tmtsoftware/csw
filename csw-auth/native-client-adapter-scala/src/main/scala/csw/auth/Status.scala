package csw.auth

private[auth] object Status extends Enumeration {
  type Status = Value
  val LOGGED_MANUAL, LOGGED_DESKTOP = Value
}
