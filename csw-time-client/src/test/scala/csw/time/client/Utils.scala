package csw.time.client

object Utils {
  def digits(d: Double): Int = BigDecimal(d).precision
}
