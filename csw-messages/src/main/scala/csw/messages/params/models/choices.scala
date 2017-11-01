package csw.messages.params.models

import java.util

import com.trueaccord.scalapb.TypeMapper
import play.api.libs.json.{Json, OFormat}

import scala.annotation.varargs
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.language.implicitConversions

/**
 * Represents a single choice
 */
case class Choice(name: String) {
  override def toString: String = name
}

/**
 * Provides implicit conversion from String to Choice
 */
object Choice {
  implicit def toChoice(name: String): Choice         = new Choice(name)
  implicit val choiceFormat: OFormat[Choice]          = Json.format[Choice]
  implicit val typeMapper: TypeMapper[String, Choice] = TypeMapper[String, Choice](Choice.apply)(_.name)
}

/**
 * Represents a set of choices
 */
case class Choices(values: Set[Choice]) {
  def contains(one: Choice): Boolean = values.contains(one)

  override def toString: String = values.mkString("(", ",", ")")

  def jValues(): util.List[Choice] = values.toList.asJava
}

/**
 * Provides a varargs constructor for Choices
 */
object Choices {
  @varargs
  def from(choicesIn: String*): Choices = Choices(choicesIn.map(Choice(_)).toSet)

  @varargs
  def fromChoices(choicesIn: Choice*): Choices = Choices(choicesIn.toSet)
  implicit val choicesFormat: OFormat[Choices] = Json.format[Choices]
}
