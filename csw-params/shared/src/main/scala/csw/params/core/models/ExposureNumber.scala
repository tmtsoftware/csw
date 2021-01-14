package csw.params.core.models

case class ExposureNumber(exposureNumber: Int, subArray: Option[Int] = None) {
  override def toString: String =
    (exposureNumber, subArray) match {
      case (exposureNumber, Some(subArray)) => s"${exposureNumber.formatted("%04d")}-${subArray.formatted("%02d")}"
      case (exposureNumber, None)           => s"${exposureNumber.formatted("%04d")}"
    }
}

object ExposureNumber {
  private def parseToInt(exposureNo: String, allowedLength: Int): Int = {
    require(
      exposureNo.length == allowedLength && exposureNo.toIntOption.isDefined,
      s"Invalid exposure number $exposureNo: exposure number should be provided $allowedLength digit." +
        " ExposureNumber should be 4 digit number and optional 2 digit sub array in format XXXX-XX or XXXX"
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
