package csw.params.events

import csw.params.core.models.Choice
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed abstract class OperationalState extends EnumEntry

object OperationalState extends Enum[OperationalState] {
  override def values: immutable.IndexedSeq[OperationalState] = findValues

  case object READY extends OperationalState

  case object ERROR extends OperationalState

  case object BUSY extends OperationalState

  def toChoices: Seq[Choice] = OperationalState.values.map(x => Choice(x.entryName))
}

object JOperationalState {
  val READY: OperationalState = OperationalState.READY
  val ERROR: OperationalState = OperationalState.ERROR
  val BUSY: OperationalState  = OperationalState.BUSY
}
