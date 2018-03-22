package csw.framework.components.hcd;

import akka.actor.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.scaladsl.CurrentStatePublisher;
import csw.messages.framework.ComponentInfo;
import csw.messages.scaladsl.TopLevelActorMessage;
import csw.services.command.scaladsl.CommandResponseManager;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.JLoggerFactory;

//#jcomponent-factory
public class JHcdComponentBehaviorFactory extends JComponentBehaviorFactory {

    @Override
    public JComponentHandlers jHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            CommandResponseManager commandResponseManager,
            CurrentStatePublisher currentStatePublisher,
            ILocationService locationService,
            JLoggerFactory loggerFactory
    ) {
        return new JHcdComponentHandlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory);
    }
}
//#jcomponent-factory
