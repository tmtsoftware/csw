package csw.event.cli.args

import com.github.tototoshi.csv.CSVParser
import csw.event.cli.args.Separators._
import csw.params.core.generics.KeyType.{BooleanKey, DoubleKey, FloatKey, IntKey, LongKey, StringKey}
import csw.params.core.generics.Parameter

object ParameterArgParser {

  def parse(cmdLineParamsArg: String): Set[Parameter[_]] =
    extractParams(cmdLineParamsArg)
      .map(keyValue)
      .map((createParam _).tupled)
      .toSet

  private def extractParams(paramsStr: String) = paramsStr.split(PARAMS_SEP)

  private def keyValue(paramStr: String) =
    paramStr.split(KEY_VALUE_SEP).toList match {
      case List(key, values) => KeyArg(key) -> parseValues(values)
      case _ =>
        throw new RuntimeException(
          s"Values are not provided in parameter arg [$paramStr], please specify param argument like keyName:keyType:unit=v1,v2"
        )
    }

  private def parseValues(values: String) = {
    val trimmedValues = trim(values, VALUES_OPENING, VALUES_CLOSING)

    CSVParser.parse(trimmedValues, DEFAULT_ESC_CHAR, VALUES_DELIMITER, VALUES_QUOTE_CHAR) match {
      case Some(result) => result.toArray
      case None         => throw new RuntimeException(s"Failed to parse values: $values")
    }
  }

  private def createParam(keyArg: KeyArg, values: Array[String]): Parameter[_] = {
    import keyArg._
    keyType match {
      case 'i' => IntKey.make(keyName, units).setAll(values.map(_.toInt))
      case 's' => StringKey.make(keyName, units).setAll(values.map(_.toString))
      case 'f' => FloatKey.make(keyName, units).setAll(values.map(_.toFloat))
      case 'd' => DoubleKey.make(keyName, units).setAll(values.map(_.toDouble))
      case 'l' => LongKey.make(keyName, units).setAll(values.map(_.toLong))
      case 'b' => BooleanKey.make(keyName).setAll(values.map(_.toBoolean))
      case _ => throw new RuntimeException(s"""
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

  private def ltrim(prefix: String): String => String                     = _.replaceAll(s"^$prefix+", "")
  private def rtrim(suffix: String): String => String                     = _.replaceAll(s"$suffix+$$", "")
  private def trim(input: String, prefix: String, suffix: String): String = (ltrim(prefix) andThen rtrim(suffix))(input)
}
