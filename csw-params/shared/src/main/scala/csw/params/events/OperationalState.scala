package csw.params.events

object OperationalState extends Enumeration {
  val READY = Value("READY")
  val ERROR = Value("ERROR")
  val BUSY  = Value("BUSY")
}
