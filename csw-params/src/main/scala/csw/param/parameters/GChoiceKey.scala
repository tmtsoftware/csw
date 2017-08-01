package csw.param.parameters

import csw.param.UnitsOfMeasure.Units
import csw.param.models.{Choice, Choices}

import scala.collection.immutable.Vector

private[parameters] class GChoiceKey(name: String, keyType: KeyType[Choice], val choices: Choices)
    extends Key[Choice](name, keyType) {
  private def validate(xs: Seq[Choice]) =
    assert(xs.forall(choices.contains), s"Bad choice for key: $keyName which must be one of: $choices")

  override def set(v: Vector[Choice], units: Units): Parameter[Choice] = {
    validate(v)
    super.set(v, units)
  }

  override def set(xs: Choice*): Parameter[Choice] = {
    validate(xs)
    super.set(xs: _*)
  }

  override def gset(v: Array[Choice], units: Units): Parameter[Choice] = {
    validate(v)
    super.gset(v, units)
  }
}
