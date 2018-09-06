package romaine.codec

trait RomaineStringCodec[T] {
  def toString(x: T): String
  def fromString(string: String): T
}

object RomaineStringCodec {

  implicit class FromString(val string: String) extends AnyVal {
    def as[A: RomaineStringCodec]: A = implicitly[RomaineStringCodec[A]].fromString(string)
  }

  implicit class ToString[A](val x: A) extends AnyVal {
    def asString(implicit c: RomaineStringCodec[A]): String = c.toString(x)
  }

  implicit val stringRomaineCodec: RomaineStringCodec[String] = codec(identity, identity)

  def codec[T](encode: T ⇒ String, decode: String ⇒ T): RomaineStringCodec[T] = new RomaineStringCodec[T] {
    override def toString(value: T): String    = encode(value)
    override def fromString(string: String): T = decode(string)
  }

}
