package csw.params.core.models

case class ExposureNumber(exposureNumber: Int, subArray: Option[Int] = None) {
  override def toString: String =
    (exposureNumber, subArray) match {
      case (exposureNumber, Some(subArray)) => s"${exposureNumber.formatted("%04d")}-${subArray.formatted("%02d")}"
      case (exposureNumber, None)           => s"${exposureNumber.formatted("%04d")}"
    }
}

object ExposureNumber {
  private def validateExposureArray(exposureArrayNo: String): Int = {
    require(
      exposureArrayNo.length == 4 && exposureArrayNo.toIntOption.isDefined,
      s"Invalid exposure number ${exposureArrayNo}: exposure number should be provided 4 digit with value between 0001 to 1000"
    )
    exposureArrayNo.toInt
  }

  private def validateSubArray(exposureNoSubArray: String): Int = {
    require(
      exposureNoSubArray.length == 2 && exposureNoSubArray.toIntOption.isDefined,
      s"Invalid subArray ${exposureNoSubArray} SubArray for exposure number should be provided 2 digit with value between 01 to 10"
    )
    exposureNoSubArray.toInt
  }

  def apply(exposureNumber: String): ExposureNumber = {
    if (exposureNumber.contains('-')) {
      val Array(exposureArrayStr, exposureNoSubArrayStr) = exposureNumber.split("-")
      val exposureArray                                  = validateExposureArray(exposureArrayStr)
      val subArray                                       = validateSubArray(exposureNoSubArrayStr)
      ExposureNumber(exposureArray, Some(subArray))
    }
    else {
      val exposureArray = validateExposureArray(exposureNumber)
      ExposureNumber(exposureArray)
    }
  }
}
