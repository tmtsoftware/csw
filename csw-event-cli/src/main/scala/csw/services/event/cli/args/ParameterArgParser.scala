package csw.services.event.cli.args

import csw.messages.params.generics.KeyType.{BooleanKey, DoubleKey, FloatKey, IntKey, LongKey, StringKey}
import csw.messages.params.generics.Parameter

object ParameterArgParser {

  def parse(cmdLineParamsArg: String): Set[Parameter[_]] =
    strParams(cmdLineParamsArg)
      .map(keyValue)
      .map((createParam _).tupled)
      .toSet

  private def strParams(paramsStr: String) = paramsStr.split('|')

  private def keyValue(paramStr: String) = paramStr.split("=").toList match {
    case List(key, values) ⇒
      val keyArg = KeyArg(key)
      val sep    = if (keyArg.keyType == 's') '\'' else ','
      keyArg → parseValues(values, sep)
    case _ ⇒
      throw new RuntimeException(
        s"Values are not provided in parameter arg [$paramStr], please specify param argument like keyName:keyType:unit=v1,v2"
      )
  }

  private def invalidValue(value: String) = value.equals(",") || value.isEmpty
  private def parseValues(values: String, separator: Char) =
    values.split(separator).map(trim("\\[", "\\]")).filterNot(invalidValue)

  private def ltrim(prefix: String): String ⇒ String                = _.replaceAll(s"^$prefix+", "")
  private def rtrim(suffix: String): String ⇒ String                = _.replaceAll(s"$suffix+$$", "")
  private def trim(prefix: String, suffix: String): String ⇒ String = ltrim(prefix) andThen rtrim(suffix)

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

}
