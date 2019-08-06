package csw.command.client

import akka.actor.typed.ActorSystem
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.models.AkkaLocation

object SequencerCommandServiceFactory {

  def make(sequencerLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]): SequencerCommandServiceImpl =
    new SequencerCommandServiceImpl(sequencerLocation)
}
