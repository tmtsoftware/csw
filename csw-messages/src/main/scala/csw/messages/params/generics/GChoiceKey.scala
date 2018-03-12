package csw.messages.params.generics

import csw.messages.params.models.{Choice, Choices, Units}

/**
 * A key for a choice item similar to an enumeration
 *
 * @param name  the name of the key
 * @param keyType reference to an object of type KeyType[Choice]
 * @param choices the available choices, the values set must be in the choices
 */
class GChoiceKey(name: String, keyType: KeyType[Choice], val choices: Choices) extends Key[Choice](name, keyType) {

  /**
   * validates the input Seq of choices
   *
   * @param xs one or more values
   */
  private def validate(xs: Seq[Choice]): Unit =
    assert(xs.forall(choices.contains), s"Bad choice for key: $keyName which must be one of: $choices")

  /**
   * Sets the values for the key using a variable number of arguments
   *
   * @param xs one or more values
   * @return a parameter containing the key name, values
   */
  override def set(xs: Choice*): Parameter[Choice] = {
    validate(xs)
    super.set(xs: _*)
  }

  /**
   * Sets the values for the key using an Array and Units
   *
   * @param v Array of Choice
   * @param units Units
   * @return a parameter containing the key name, values, specified unit
   */
  override def set(v: Array[Choice], units: Units): Parameter[Choice] = {
    validate(v)
    super.set(v, units)
  }
}
