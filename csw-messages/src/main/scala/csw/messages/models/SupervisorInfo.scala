package csw.messages.models

import akka.actor.ActorSystem

//TODO: what, why, how
case class SupervisorInfo(system: ActorSystem, component: Component)
