package csw.framework.components.hcd;

import akka.actor.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.command.client.messages.TopLevelActorMessage;

//#jcomponent-factory
public class JHcdComponentBehaviorFactory extends JComponentBehaviorFactory {
    @Override
    public JComponentHandlers jHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        return new JHcdComponentHandlers(ctx, cswCtx);
    }
}
//#jcomponent-factory
