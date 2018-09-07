package csw.framework.components.assembly;

import akka.actor.typed.javadsl.ActorContext;
import csw.framework.CurrentStatePublisher;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.messages.TopLevelActorMessage;
import csw.messages.framework.ComponentInfo;
import csw.services.command.CommandResponseManager;

//#jcomponent-factory
public class JAssemblyComponentBehaviorFactory extends JComponentBehaviorFactory {

    @Override
    public JComponentHandlers jHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            JCswContext cswCtx
    ) {
        return new JAssemblyComponentHandlers(ctx, componentInfo, cswCtx);
    }
}
//#jcomponent-factory
