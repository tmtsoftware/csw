package csw.services.event.cli

import csw.messages.params.generics.KeyType.{BooleanKey, DoubleKey, FloatKey, IntKey, LongKey, StringKey}
import csw.messages.params.generics.Parameter

object ParameterArgParser {

  def parse(cmdLineParamsArg: String): Set[Parameter[_]] =
    strParams(cmdLineParamsArg)
      .map(keyValue)
      .map { case (key, values) ⇒ createParam(KeyArg(key), values) }
      .toSet

  private def strParams(paramsStr: String) = paramsStr.split(" ")

  private def keyValue(paramStr: String) = paramStr.split("=").toList match {
    case List(key, value) ⇒ key → value.split(",")
    case _ ⇒
      throw new RuntimeException(
        s"Values are not provided in parameter arg [$paramStr], please specify param argument like keyName:keyType:unit=v1,v2"
      )
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
      case _   ⇒ throw new RuntimeException(s"Unsupported key type [${keyArg.keyType}] provided")
    }
  }

}
