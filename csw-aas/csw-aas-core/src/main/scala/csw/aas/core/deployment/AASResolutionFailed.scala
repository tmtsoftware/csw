package csw.aas.core.deployment

/**
 * Indicates that an attempt to resolve AAS service via location service was failed
 */
case class AASResolutionFailed(msg: String) extends Exception(msg)
