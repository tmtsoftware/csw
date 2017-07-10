package csw.services.logging.commons

private[logging] object Keys {

  val CATEGORY       = "@category"
  val SEVERITY       = "@severity"
  val COMPONENT_NAME = "@componentName"
  val TRACE_ID       = "@traceId"
  val MSG            = "@msg"
  val TIMESTAMP      = "timestamp"
  val MESSAGE        = "message"
  val ACTOR          = "actor"
  val FILE           = "file"
  val CLASS          = "class"
  val LINE           = "line"
  val KIND           = "kind"
  val EX             = "ex"
  val HOST           = "@host"
  val SERVICE        = "@service"
  val NAME           = "@name"
  val VERSION        = "@version"
  // File rotation hour of the day (This is considered as UTC Time)
  val FILE_ROTATION_HOUR: Long = 12L

}
