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

  //////////////

  private val ParameterSetMapper =
    TypeMapper[Seq[PbParameter], Set[Parameter[_]]] {
      _.map(TypeMapperSupport.parameterTypeMapper2.toCustom).toSet
    } {
      _.map(TypeMapperSupport.parameterTypeMapper2.toBase).toSeq
    }

  /**
   * TypeMapper definitions are required for to/from conversion PbEvent(Protobuf) <==> System, Observe event.
   */
  private[csw] implicit def eventTypeMapper[T <: Event]: TypeMapper[PbEvent, T] = new TypeMapper[PbEvent, T] {
    override def toCustom(base: PbEvent): T = {
      val factory: (Id, Prefix, EventName, EventTime, Set[Parameter[_]]) ⇒ Any = base.eventType match {
        case PbEventType.SystemEvent     ⇒ SystemEvent.apply
        case PbEventType.ObserveEvent    ⇒ ObserveEvent.apply
        case PbEventType.Unrecognized(x) ⇒ throw new RuntimeException(s"unknown event type=[${base.eventType.toString} :$x]")
      }

      factory(
        Id(base.eventId),
        Prefix(base.source),
        EventName(base.name),
        base.eventTime.map(EventTime.typeMapper.toCustom).get,
        ParameterSetMapper.toCustom(base.paramSet)
      ).asInstanceOf[T]
    }

    override def toBase(custom: T): PbEvent = {
      val pbEventType = custom match {
        case _: ObserveEvent ⇒ PbEventType.ObserveEvent
        case _: SystemEvent  ⇒ PbEventType.SystemEvent
      }
      PbEvent()
        .withEventId(custom.eventId.id)
        .withSource(custom.source.prefix)
        .withName(custom.eventName.name)
        .withEventTime(EventTime.typeMapper.toBase(custom.eventTime))
        .withParamSet(ParameterSetMapper.toBase(custom.paramSet))
        .withEventType(pbEventType)
    }
  }

}
