package csw.param.pb

import com.google.protobuf.ByteString

trait PbFormat[T] {
  def read(bytes: ByteString): T
  def write(x: T): ByteString
}

object PbFormat {
  def apply[T](implicit x: PbFormat[T]): PbFormat[T] = x
}
