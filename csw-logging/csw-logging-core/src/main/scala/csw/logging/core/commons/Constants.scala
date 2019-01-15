package csw.logging.core.commons

private[logging] object Constants {

  // file rotation hour of the day (This is considered as UTC Time)
  val FILE_ROTATION_HOUR: Long = 12L

  // Key to default logging state. If component specific logging is not specified then default logging state
  // stored against this key will be used by default.
  val DEFAULT_KEY = "default"

}
