package csw.framework.components

import csw.framework.exceptions.FailureStop

case class ConfigNotAvailableException() extends FailureStop("Configuration not available. Initialization failure.")
