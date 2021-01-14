package csw.params.core.models

case class ExposureNumber(exposureNumber: Int, subArray: Option[Int] = None) {
  override def toString: String =
    (exposureNumber, subArray) match {
      case (exposureNumber, Some(subArray)) => s"${exposureNumber.formatted("%04d")}-${subArray.formatted("%02d")}"
      case (exposureNumber, None)           => s"${exposureNumber.formatted("%04d")}"
    }
}

object ExposureNumber {
  val SEPARATOR = '-'
  private def validate(exposureNo: String, allowedLength: Int): Int = {
    require(
      exposureNo.length == allowedLength && exposureNo.toIntOption.isDefined,
      s"Invalid exposure number ${exposureNo}: exposure number should be provided ${allowedLength} digit." +
        " ExposureNumber should be 4 digit number and optional 2 digit sub array in format XXXX-XX or XXXX"
    )
    exposureNo.toInt
  }

  def apply(exposureNumber: String): ExposureNumber = {
    if (exposureNumber.count(_ == SEPARATOR) == 1) {
      val Array(exposureArrayStr, exposureNoSubArrayStr) = exposureNumber.split("-")
      val exposureArray                                  = validate(exposureArrayStr, allowedLength = 4)
      val subArray                                       = validate(exposureNoSubArrayStr, allowedLength = 2)
      ExposureNumber(exposureArray, Some(subArray))
    }
    else {
      ExposureNumber(validate(exposureNumber, allowedLength = 4))
    }
  }
}
