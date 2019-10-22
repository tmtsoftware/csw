package csw.command.client.models.framework

import akka.actor.typed.{ActorSystem, SpawnProtocol}

/**
 * SupervisorInfo is used by container while spawning multiple components
 *
 * @param system an ActorSystem used for spawning actors of a component. Each component has it's own actorSystem. The container
 *               keeps all the actorSystems for all components with it and shuts them down when a Shutdown message is
 *               received by a container.
 * @param component represents a supervisor actor reference and componentInfo
 */
private[csw] case class SupervisorInfo(system: ActorSystem[SpawnProtocol.Command], component: Component)
