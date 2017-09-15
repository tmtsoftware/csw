package csw.param.pb

import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message, TypeMapper}
import csw.param.generics.KeyType

import scala.reflect.ClassTag

trait ItemType[T, S <: ItemType[T, S]] extends GeneratedMessage with Message[S] {
  def values: Seq[T]
  def withValues(xs: Seq[T]): S
  def keyType(implicit tag: ClassTag[T]): KeyType[T] = KeyType.values.find(_.tag == tag).get.asInstanceOf[KeyType[T]]
  def as[R](implicit mapper: TypeMapper[S, R]): R    = mapper.toCustom(this.asInstanceOf[S])
}

//trait ItemTypeCompanion {
//  def from[T](_values: Seq[T]): ItemType[T] = new ItemType[T] {
//    override def values: Seq[T] = _values
//  }
//}
