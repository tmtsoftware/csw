package csw.event.client.pb

import com.google.protobuf.timestamp.Timestamp
import csw.event.client.pb.Implicits.instantMapper
import csw.params.events._
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.ObsId.empty
import csw.params.core.models._
import csw.event.client.pb.TypeMapperFactory.make
import csw.time.api.UTCTime
import csw_protobuf.events.PbEvent
import csw_protobuf.events.PbEvent.PbEventType
import csw_protobuf.keytype.PbKeyType
import csw_protobuf.parameter.PbParameter.Items
import csw_protobuf.parameter.{PbParameter, PbStruct}
import csw_protobuf.radec.PbRaDec
import csw_protobuf.units.PbUnits
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

  private val ParameterSetMapper =
    TypeMapper[Seq[PbParameter], Set[Parameter[_]]] {
      _.map(TypeMapperSupport.parameterTypeMapper2.toCustom).toSet
    } {
      _.map(TypeMapperSupport.parameterTypeMapper2.toBase).toSeq
    }

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
        base.eventTime.map(eventTimeTypeMapper.toCustom).get,
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
        .withEventTime(eventTimeTypeMapper.toBase(custom.eventTime))
        .withParamSet(ParameterSetMapper.toBase(custom.paramSet))
        .withEventType(pbEventType)
    }
  }

  private implicit val eventTimeTypeMapper: TypeMapper[Timestamp, EventTime] =
    TypeMapper[Timestamp, EventTime] { x ⇒
      EventTime(UTCTime(instantMapper.toCustom(x)))
    } { x ⇒
      instantMapper.toBase(x.time.value)
    }

  implicit val structTypeMapper: TypeMapper[PbStruct, Struct] = TypeMapper[PbStruct, Struct] { s =>
    Struct(s.paramSet.map(parameterTypeMapper2.toCustom).toSet)
  } { s =>
    PbStruct().withParamSet(s.paramSet.map(TypeMapperSupport.parameterTypeMapper2.toBase).toSeq)
  }

  implicit val choiceTypeMapper: TypeMapper[String, Choice] = TypeMapper[String, Choice](Choice.apply)(_.name)

  implicit val unitsTypeMapper: TypeMapper[PbUnits, Units] =
    TypeMapper[PbUnits, Units](x ⇒ Units.withName(x.toString()))(x ⇒ PbUnits.fromName(x.toString).get)

  implicit def matrixDataTypeMapper[T: ClassTag, S <: ItemType[ArrayData[T]]: ItemTypeCompanion]: TypeMapper[S, MatrixData[T]] =
    TypeMapper[S, MatrixData[T]](x ⇒ MatrixData.fromArrays(x.values.toArray.map(a ⇒ a.data.array)))(
      x ⇒ ItemTypeCompanion.make(x.data.map(ArrayData.apply))
    )

  implicit val raDecTypeMapper: TypeMapper[PbRaDec, RaDec] =
    TypeMapper[PbRaDec, RaDec](x ⇒ RaDec(x.ra, x.dec))(x ⇒ PbRaDec().withRa(x.ra).withDec(x.dec))

  implicit val keyTypeTypeMapper: TypeMapper[PbKeyType, KeyType[_]] =
    TypeMapper[PbKeyType, KeyType[_]](x ⇒ KeyType.withName(x.toString()))(x ⇒ PbKeyType.fromName(x.toString).get)

  implicit def arrayDataTypeMapper[T: ClassTag, S <: ItemType[T]: ItemTypeCompanion]: TypeMapper[S, ArrayData[T]] =
    TypeMapper[S, ArrayData[T]](x ⇒ ArrayData(x.values.toArray[T]))(x ⇒ ItemTypeCompanion.make(x.data))

  implicit val obsIdTypeMapper: TypeMapper[String, Option[ObsId]] = TypeMapper[String, Option[ObsId]] { x ⇒
    if (x.isEmpty) None else Some(ObsId(x))
  } { x ⇒
    x.getOrElse(empty).obsId
  }
}
