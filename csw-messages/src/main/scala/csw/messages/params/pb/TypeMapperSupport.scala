package csw.messages.params.pb

import csw.messages.events._
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.{Id, Prefix}
import csw.messages.params.pb.TypeMapperFactory.make
import csw_protobuf.events.PbEvent
import csw_protobuf.events.PbEvent.PbEventType
import csw_protobuf.parameter.PbParameter
import csw_protobuf.parameter.PbParameter.Items
import play.api.libs.json.Format
import scalapb.TypeMapper

import scala.collection.mutable
import scala.reflect.ClassTag

object TypeMapperSupport {

  implicit def parameterTypeMapper[S: ClassTag: Format: ItemsFactory]: TypeMapper[PbParameter, Parameter[S]] =
    new TypeMapper[PbParameter, Parameter[S]] {
      override def toCustom(pbParameter: PbParameter): Parameter[S] = Parameter(
        pbParameter.name,
        pbParameter.keyType.asInstanceOf[KeyType[S]],
        cswItems(pbParameter.items),
        pbParameter.units
      )

      override def toBase(x: Parameter[S]): PbParameter =
        PbParameter()
          .withName(x.keyName)
          .withUnits(x.units)
          .withKeyType(x.keyType)
          .withItems(ItemsFactory[S].make(x.items))
    }

  implicit val parameterTypeMapper2: TypeMapper[PbParameter, Parameter[_]] = {
    TypeMapper[PbParameter, Parameter[_]](
      p ⇒ make(p.keyType).toCustom(p)
    )(p => make(p.keyType).toBase(p))
  }

  private def cswItems[T: ClassTag](items: Items): mutable.WrappedArray[T] = items.value match {
    case x: ItemType[_] ⇒ x.asInstanceOf[ItemType[T]].values.toArray[T]
    case x              ⇒ throw new RuntimeException(s"unexpected type ${x.getClass} found, ItemType expected")
  }
}
