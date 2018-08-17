package romaine.codec

trait RomaineStringCodec[T] {
  def toString(x: T): String
  def fromString(string: String): T
}

object RomaineStringCodec {
  def apply[T](implicit x: RomaineStringCodec[T]): RomaineStringCodec[T] = x

  implicit val stringRomaineCodec: RomaineStringCodec[String] = new RomaineStringCodec[String] {
    override def toString(x: String): String        = x
    override def fromString(string: String): String = string
  }
}
