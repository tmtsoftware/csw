package csw.param.pb

import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message, TypeMapper}
import csw.param.models.ArrayData

import scala.reflect.ClassTag

trait ItemType[T, S <: ItemType[T, S]] extends GeneratedMessage with Message[S] { self: S ⇒
  def values: Seq[T]
  def withValues(xs: Seq[T]): S
  def as[R](implicit mapper: TypeMapper[S, R]): R = mapper.toCustom(this)
//  def keyType(implicit tag: ClassTag[T]): KeyType[T] = KeyType.values.find(_.tag == tag).get.asInstanceOf[KeyType[T]]
}

//trait ItemTypeCompanion[T, S <: ItemType[T, S]] extends GeneratedMessageCompanion[S] {
//  implicit def typeMapper2(implicit tag: ClassTag[T]): TypeMapper[S, ArrayData[T]] =
//    TypeMapper[S, ArrayData[T]](x ⇒ ArrayData(x.values.toArray[T]))(x ⇒ defaultInstance.withValues(x.data))
//}
