package csw.param.pb

trait PbFormat[T] {
  def read(bytes: Array[Byte]): T
  def write(x: T): Array[Byte]
}

object PbFormat {
  def apply[T](implicit x: PbFormat[T]): PbFormat[T] = x
}
