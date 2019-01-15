package csw.event.cli.args

// Example of params: "k1:s=['Kevin O\\'Brien','Chicago, USA']|k2:i=[1,2,3,4]"
object Separators {
  val PARAMS_SEP = '|'

  val KEY_SEP       = ':'
  val KEY_VALUE_SEP = '='

  val VALUES_DELIMITER  = ','
  val VALUES_QUOTE_CHAR = '\''
  val VALUES_OPENING    = "\\["
  val VALUES_CLOSING    = "\\]"

  val DEFAULT_ESC_CHAR = '\\'
}
