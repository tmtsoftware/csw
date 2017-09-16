package csw.param.pb

import csw.param.generics._
import csw_params.parameter.PbParameter.Items
import csw_params.parameter_types.IntItems

import scala.reflect.ClassTag

trait ItemType[T] {
  def values: Seq[T]
  def withValues2[R](xs: Seq[T]): R = withValues(xs).asInstanceOf[R]
  def withValues(xs: Seq[T]): Any
  def keyType(implicit tag: ClassTag[T]): KeyType[T] = KeyType.values.find(_.tag == tag).get.asInstanceOf[KeyType[T]]
}

trait ItemTypeCompanion[S] {
  def defaultInstance: S
}

object ItemTypeCompanion {
  def apply[T](implicit x: ItemTypeCompanion[T]): ItemTypeCompanion[T] = x
}

trait ParamType {
  def items: Items
  def cswItems: ItemType[_] = items.value match {
    case x: ItemType[_] ⇒ x
    case x              ⇒ throw new RuntimeException(s"unexpected type ${x.getClass} found, ItemType expected")
  }
}

trait Mapping {
  type T
  type S <: ItemType[T]
}

object Mapping {
  type Aux[_T, _S <: ItemType[_T]] = Mapping {
    type T = _T
    type S = _S
  }

  def Aux[_T, _S <: ItemType[_T]]: Aux[_T, _S] = new Mapping {
    type T = _T
    type S = _S
  }

  implicit val IntMapping: Aux[Int, IntItems] = Aux[Int, IntItems]
}
