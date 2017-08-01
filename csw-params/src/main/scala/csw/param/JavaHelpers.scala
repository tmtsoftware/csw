package csw.param

import csw.param.Parameters.ParameterSetType
import csw.param.parameters._

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

  def jget[S, T <: ParameterSetType[T]](sc: T, key: Key[S]): java.util.Optional[GParam[S]] =
    sc.get(key).asJava

  def jget[S, I <: Parameter[S], T <: ParameterSetType[T], J](sc: T, key: Key[S], index: Int): java.util.Optional[J] = {
    sc.get(key) match {
      case Some(item) =>
        (if (index >= 0 && index < item.size) Some(item.values(index).asInstanceOf[J]) else None).asJava
      case None => None.asJava
    }
  }

  def jvalue[S, T <: ParameterSetType[T], J](sc: T, key: Key[S]): J = {
    val item = sc.get(key)
    item match {
      case Some(x) => x.values(0).asInstanceOf[J]
      case None    => throw new NoSuchElementException(s"Item: $key not found")
    }
  }

  def jvalues[S, T <: ParameterSetType[T], J](sc: T, key: Key[S]): java.util.List[J] = {
    val item = sc.get(key)
    item match {
      case Some(x) => x.values.map(i => i.asInstanceOf[J]).asJava
      case None    => throw new NoSuchElementException(s"Item: $key not found")
    }
  }
}
