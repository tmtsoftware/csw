package csw.services.alarm.api.internal

private[alarm] object Separators {
  // Use "-" as separator since hyphen is an unsupported character in subsystem, component and alarm name
  // Which enables safe parsing of AlarmKey from string.
  val KeySeparator = '-'
}
