package csw.framework.components.assembly;

import akka.actor.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.command.messages.TopLevelActorMessage;

//#jcomponent-factory
public class JAssemblyComponentBehaviorFactory extends JComponentBehaviorFactory {
    @Override
    public JComponentHandlers jHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        return new JAssemblyComponentHandlers(ctx, cswCtx);
    }
}
//#jcomponent-factory
