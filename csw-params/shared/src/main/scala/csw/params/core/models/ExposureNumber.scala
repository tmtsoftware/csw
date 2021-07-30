package csw.params.core.models

case class ExposureNumber(exposureNumber: Int, subArray: Option[Int] = None) {
  override def toString: String =
    (exposureNumber, subArray) match {
      case (exposureNumber, Some(subArray)) => s"${exposureNumber.formatted("%04d")}-${subArray.formatted("%02d")}"
      case (exposureNumber, None)           => s"${exposureNumber.formatted("%04d")}"
    }

  /** Returns the next exposure number */
  def next(): ExposureNumber = ExposureNumber(exposureNumber + 1, subArray)

  /** Returns next subarray number */
  def nextSubArray(): ExposureNumber =
    ExposureNumber(
      exposureNumber,
      subArray match {
        case None           => Some(0)
        case Some(subArray) => Some(subArray + 1)
      }
    )
}

object ExposureNumber {

  /**
   * A convenience to use when constructing ExposureId
   * @return the default ExposureNumber
   */
  def default = ExposureNumber(0)

  private def parseToInt(exposureNo: String, allowedLength: Int): Int = {
    require(
      exposureNo.length == allowedLength && exposureNo.toIntOption.isDefined,
      s"Invalid exposure number: $exposureNo. " +
        "An ExposureNumber must be a 4 digit number and optional 2 digit sub array in format XXXX or XXXX-XX"
    )
    exposureNo.toInt
  }

  def apply(exposureNumber: String): ExposureNumber =
    exposureNumber.split(Separator.Hyphen) match {
      case Array(exposureArrayStr, exposureNoSubArrayStr) =>
        val exposureArray = parseToInt(exposureArrayStr, allowedLength = 4)
        val subArray      = parseToInt(exposureNoSubArrayStr, allowedLength = 2)
        ExposureNumber(exposureArray, Some(subArray))
      case _ => ExposureNumber(parseToInt(exposureNumber, allowedLength = 4))
    }

}
