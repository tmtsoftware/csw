package csw.param.parameters

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.annotation.varargs
import scala.language.implicitConversions

/**
 * Represents a single choice
 */
case class Choice(name: String) {
  override def toString = name
}

/**
 * Provides implicit conversion from String to Choice
 */
object Choice extends DefaultJsonProtocol {
  implicit def toChoice(name: String): Choice       = new Choice(name)
  implicit val choiceFormat: RootJsonFormat[Choice] = jsonFormat1(Choice.apply)
}

/**
 * Represents a gset of choices
 */
case class Choices(values: Set[Choice]) {
  def contains(one: Choice) = values.contains(one)

  override def toString = values.mkString("(", ",", ")")
}

/**
 * Provides a varargs constructor for Choices
 */
object Choices {
  @varargs
  def from(choicesIn: String*): Choices = Choices(choicesIn.map(Choice(_)).toSet)

  @varargs
  def fromChoices(choicesIn: Choice*): Choices = Choices(choicesIn.toSet)
}
