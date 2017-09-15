package csw.param.pb

import com.trueaccord.scalapb.{GeneratedMessage, Message, TypeMapper}
import csw.param.generics._
import csw_params.parameter.PbParameter.Items

import scala.reflect.ClassTag

trait ItemType[T, S <: ItemType[T, S]] extends GeneratedMessage with Message[S] { self: S ⇒
  def values: Seq[T]
  def withValues(xs: Seq[T]): S
  def as[R](implicit mapper: TypeMapper[S, R]): R    = mapper.toCustom(this)
  def keyType(implicit tag: ClassTag[T]): KeyType[T] = KeyType.values.find(_.tag == tag).get.asInstanceOf[KeyType[T]]
}

trait ParamType {
  def items: Items
  def cswItems: ItemType[_, _] = items.value match {
    case x: ItemType[_, _] ⇒ x
    case x                 ⇒ throw new RuntimeException(s"unexpected type ${x.getClass} found, ItemType expected")
  }
}
