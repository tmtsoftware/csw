package csw.framework.components.hcd;

import akka.actor.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.CurrentStatePublisher;
import csw.framework.models.JCswContext;
import csw.messages.framework.ComponentInfo;
import csw.messages.TopLevelActorMessage;
import csw.services.command.CommandResponseManager;

//#jcomponent-factory
public class JHcdComponentBehaviorFactory extends JComponentBehaviorFactory {

    @Override
    public JComponentHandlers jHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            CommandResponseManager commandResponseManager,
            CurrentStatePublisher currentStatePublisher,
            JCswContext cswCtx
    ) {
        return new JHcdComponentHandlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, cswCtx);
    }
}
//#jcomponent-factory
