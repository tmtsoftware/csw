package csw.command.client.cbor

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import csw.command.client.messages._
import csw.command.client.messages.sequencer.CswSequencerMessage
import csw.command.client.models.framework._
import csw.params.commands.CommandResponse
import csw.params.core.states.StateVariable
import csw.serializable.CommandSerializable

class CommandAkkaSerializer(_system: ExtendedActorSystem) extends CborAkkaSerializer[CommandSerializable] with MessageCodecs {

  override implicit def actorSystem: ActorSystem[_] = _system.toTyped

  override def identifier: Int = 19923

  register[CommandSerializationMarker.RemoteMsg]
  register[CommandResponse]
  register[StateVariable]
  register[SupervisorLifecycleState]
  register[ContainerLifecycleState]
  register[LifecycleStateChanged]
  register[Components]
  register[LockingResponse]
  register[CswSequencerMessage]
}
