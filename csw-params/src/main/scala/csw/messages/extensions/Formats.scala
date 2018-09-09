package csw.messages.extensions
import play.api.libs.json.{Format, Json, Writes}

object Formats {
  def of[T](implicit x: Format[T]): Format[T] = x

  implicit class MappableFormat[A](format: Format[A]) {
    def bimap[B](to: B => A, from: A => B): Format[B] = Format[B](
      format.map(from),
      Writes[B](x => Json.toJson(to(x))(format))
    )
  }
}
