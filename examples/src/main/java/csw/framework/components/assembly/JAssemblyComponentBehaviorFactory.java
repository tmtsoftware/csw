package csw.framework.components.assembly;

import akka.actor.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswServices;
import csw.messages.TopLevelActorMessage;
import csw.messages.framework.ComponentInfo;

//#jcomponent-factory
public class JAssemblyComponentBehaviorFactory extends JComponentBehaviorFactory {

    @Override
    public JComponentHandlers jHandlers(
            ActorContext<TopLevelActorMessage> ctx,
            ComponentInfo componentInfo,
            JCswServices cswCtx
    ) {
        return new JAssemblyComponentHandlers(ctx, componentInfo, cswCtx);
    }
}
//#jcomponent-factory
