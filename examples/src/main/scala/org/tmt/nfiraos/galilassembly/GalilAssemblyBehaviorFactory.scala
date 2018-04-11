//package org.tmt.nfiraos.galilassembly
//
//import akka.actor.typed.scaladsl.ActorContext
//import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers, CurrentStatePublisher}
//import csw.messages.framework.ComponentInfo
//import csw.messages.scaladsl.TopLevelActorMessage
//import csw.services.command.scaladsl.CommandResponseManager
//import csw.services.location.scaladsl.LocationService
//import csw.services.logging.scaladsl.LoggerFactory
//
//class GalilAssemblyBehaviorFactory extends ComponentBehaviorFactory {
//
//  override def handlers(
//      ctx: ActorContext[TopLevelActorMessage],
//      componentInfo: ComponentInfo,
//      commandResponseManager: CommandResponseManager,
//      currentStatePublisher: CurrentStatePublisher,
//      locationService: LocationService,
//      loggerFactory: LoggerFactory
//  ): ComponentHandlers =
//    new GalilAssemblyHandlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory)
//
//}
