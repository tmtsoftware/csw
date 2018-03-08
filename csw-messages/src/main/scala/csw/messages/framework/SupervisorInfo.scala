package csw.messages.framework

import akka.actor.ActorSystem

//TODO: what, why, how
case class SupervisorInfo(system: ActorSystem, component: Component)
