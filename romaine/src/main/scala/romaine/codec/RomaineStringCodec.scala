package romaine.codec

trait RomaineStringCodec[T] {
  def toString(x: T): String
  def fromString(string: String): T
}

object RomaineStringCodec {
  def apply[T](implicit x: RomaineStringCodec[T]): RomaineStringCodec[T] = x

  def codec[T](encode: T ⇒ String, decode: String ⇒ T): RomaineStringCodec[T] = new RomaineStringCodec[T] {
    override def toString(value: T): String    = encode(value)
    override def fromString(string: String): T = decode(string)
  }

  implicit val stringRomaineCodec: RomaineStringCodec[String] = codec(identity, identity)
}
