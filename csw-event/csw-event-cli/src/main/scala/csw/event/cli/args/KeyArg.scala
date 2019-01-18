package csw.event.cli.args

import csw.event.cli.args.Separators.KEY_SEP
import csw.params.core.models.Units
import csw.params.core.models.Units.NoUnits

case class KeyArg private (keyName: String, keyType: Char, units: Units)

object KeyArg {

  def apply(keyStr: String): KeyArg = keyStr.split(KEY_SEP).toList match {
    case List(keyName, keyType, unit) ⇒ KeyArg(keyName, keyType.charAt(0), Units.withNameInsensitive(unit))
    case List(keyName, keyType)       ⇒ KeyArg(keyName, keyType.charAt(0), NoUnits)
    case _ ⇒
      throw new RuntimeException(
        s"Invalid key [$keyStr] provided. Please specify at least Key name and key type like keyName:keyType (ex. k1:s)"
      )
  }

}
