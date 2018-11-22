package example
import julienrf.json.derived
import play.api.libs.json.{__, OFormat}

case class Person(name: String, city: String, country: String)

object Person {
  implicit val personFormat: OFormat[Person] = derived.flat.oformat((__ \ "type").format[String])
}
