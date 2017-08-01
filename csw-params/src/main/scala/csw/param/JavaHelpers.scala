package csw.param

import csw.param.Parameters.ParameterSetType
import csw.param.UnitsOfMeasure.Units
import csw.param.parameters._

import scala.annotation.varargs
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/**
 * TMT Source Code: 6/23/16.
 */
private[param] object JavaHelpers {

  def jadd[I <: Parameter[_], T <: ParameterSetType[T]](sc: T, items: java.util.List[I]): T = {
    val x = items.asScala
    x.foldLeft(sc)((r, i) => r.add(i))
  }

  def jget[S, I <: Parameter[S], T <: ParameterSetType[T]](sc: T, key: Key[S, I]): java.util.Optional[I] =
    sc.get(key).asJava

  def jget[S, I <: Parameter[S], T <: ParameterSetType[T], J](sc: T,
                                                              key: Key[S, I],
                                                              index: Int): java.util.Optional[J] = {
    sc.get(key) match {
      case Some(item) =>
        (if (index >= 0 && index < item.size) Some(item.values(index).asInstanceOf[J]) else None).asJava
      case None => None.asJava
    }
  }

  def jvalue[S, I <: Parameter[S], T <: ParameterSetType[T], J](sc: T, key: Key[S, I]): J = {
    val item = sc.get(key)
    item match {
      case Some(x) => x.values(0).asInstanceOf[J]
      case None    => throw new NoSuchElementException(s"Item: $key not found")
    }
  }

  def jvalues[S, I <: Parameter[S], T <: ParameterSetType[T], J](sc: T, key: Key[S, I]): java.util.List[J] = {
    val item = sc.get(key)
    item match {
      case Some(x) => x.values.map(i => i.asInstanceOf[J]).asJava
      case None    => throw new NoSuchElementException(s"Item: $key not found")
    }
  }

  // ChoiceItem
  def jvalue(item: ChoiceParameter): Choice = item.values(0)

  def jvalue(item: ChoiceParameter, index: Int): Choice = item.values(index)

  def jvalues(item: ChoiceParameter): java.util.List[Choice] = item.values.map(i => i: Choice).asJava

  def jget(item: ChoiceParameter, index: Int): java.util.Optional[Choice] = item.get(index).map(i => i: Choice).asJava

  def jset(key: ChoiceKey, v: java.util.List[Choice], units: Units): ChoiceParameter =
    ChoiceParameter(key.keyName, key.choices, v.asScala.toVector.map(i => i: Choice), units)

  @varargs
  def jset(key: ChoiceKey, v: Choice*) =
    ChoiceParameter(key.keyName, key.choices, v.map(i => i: Choice).toVector, units = UnitsOfMeasure.NoUnits)

  // StructItem
  def jvalue(item: StructParameter): Struct = item.values(0)

  def jvalue(item: StructParameter, index: Int): Struct = item.values(index)

  def jvalues(item: StructParameter): java.util.List[Struct] = item.values.map(i => i: Struct).asJava

  def jget(item: StructParameter, index: Int): java.util.Optional[Struct] = item.get(index).map(i => i: Struct).asJava

  def jset(key: StructKey, v: java.util.List[Struct], units: Units): StructParameter =
    StructParameter(key.keyName, v.asScala.toVector.map(i => i: Struct), units)

  @varargs
  def jset(key: StructKey, v: Struct*) =
    StructParameter(key.keyName, v.map(i => i: Struct).toVector, units = UnitsOfMeasure.NoUnits)
}
