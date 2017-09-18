package csw.param.pb

import csw.param.generics._
import csw_params.parameter.PbParameter.Items

import scala.reflect.ClassTag

trait ItemType[T] {
  def values: Seq[T]
  def withValues(xs: Seq[T]): Any
  def set(xs: Seq[T]): this.type                     = withValues(xs).asInstanceOf[this.type]
  def keyType(implicit tag: ClassTag[T]): KeyType[T] = KeyType.values.find(_.tag == tag).get.asInstanceOf[KeyType[T]]
}

trait ItemTypeCompanion[S] {
  def defaultInstance: S
}

object ItemTypeCompanion {
  def apply[T](implicit x: ItemTypeCompanion[T]): ItemTypeCompanion[T] = x
  def make[T, S <: ItemType[T]: ItemTypeCompanion](items: Seq[T]): S = {
    ItemTypeCompanion[S].defaultInstance.set(items)
  }
}

trait ParamType {
  def items: Items
  def cswItems: ItemType[_] = items.value match {
    case x: ItemType[_] ⇒ x
    case x              ⇒ throw new RuntimeException(s"unexpected type ${x.getClass} found, ItemType expected")
  }
}
