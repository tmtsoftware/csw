package csw.messages.models

import akka.actor.ActorSystem

case class SupervisorInfo(system: ActorSystem, component: Component)
