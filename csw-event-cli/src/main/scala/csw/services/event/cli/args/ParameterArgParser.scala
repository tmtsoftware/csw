package csw.services.event.cli.args

import com.github.tototoshi.csv.CSVParser
import csw.messages.params.generics.KeyType.{BooleanKey, DoubleKey, FloatKey, IntKey, LongKey, StringKey}
import csw.messages.params.generics.Parameter
import csw.services.event.cli.args.Separators._

object ParameterArgParser {

  def parse(cmdLineParamsArg: String): Set[Parameter[_]] =
    extractParams(cmdLineParamsArg)
      .map(keyValue)
      .map((createParam _).tupled)
      .toSet

  private def extractParams(paramsStr: String) = paramsStr.split(PARAMS_SEP)

  private def keyValue(paramStr: String) = paramStr.split(KEY_VALUE_SEP).toList match {
    case List(key, values) ⇒ KeyArg(key) → parseValues(values)
    case _ ⇒
      throw new RuntimeException(
        s"Values are not provided in parameter arg [$paramStr], please specify param argument like keyName:keyType:unit=v1,v2"
      )
  }

  private def parseValues(values: String) = {
    val trimmedValues = trim(values, VALUES_OPENING, VALUES_CLOSING)

    CSVParser.parse(trimmedValues, DEFAULT_ESC_CHAR, VALUES_DELIMITER, VALUES_QUOTE_CHAR) match {
      case Some(result) ⇒ result.toArray
      case None         ⇒ throw new RuntimeException(s"Failed to parse values: $values")
    }
  }

  private def createParam(keyArg: KeyArg, values: Array[String]): Parameter[_] = {
    import keyArg._
    keyType match {
      case 'i' ⇒ IntKey.make(keyName).set(values.map(_.toInt), units)
      case 's' ⇒ StringKey.make(keyName).set(values.map(_.toString), units)
      case 'f' ⇒ FloatKey.make(keyName).set(values.map(_.toFloat), units)
      case 'd' ⇒ DoubleKey.make(keyName).set(values.map(_.toDouble), units)
      case 'l' ⇒ LongKey.make(keyName).set(values.map(_.toLong), units)
      case 'b' ⇒ BooleanKey.make(keyName).set(values.map(_.toBoolean), units)
      case _   ⇒ throw new RuntimeException(s"""
           |Unsupported key type [${keyArg.keyType}] provided.
           |Supported key types are:
           |i = IntKey
           |s = StringKey
           |f = FloatKey
           |d = DoubleKey
           |l = LongKey
           |b = BooleanKey
           |""".stripMargin)
    }
  }

  private def ltrim(prefix: String): String ⇒ String                      = _.replaceAll(s"^$prefix+", "")
  private def rtrim(suffix: String): String ⇒ String                      = _.replaceAll(s"$suffix+$$", "")
  private def trim(input: String, prefix: String, suffix: String): String = (ltrim(prefix) andThen rtrim(suffix))(input)
}
