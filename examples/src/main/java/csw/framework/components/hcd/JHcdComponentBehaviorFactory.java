package csw.framework.components.hcd;

import akka.actor.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswServices;
import csw.messages.framework.ComponentInfo;
import csw.messages.TopLevelActorMessage;

//#jcomponent-factory
public class JHcdComponentBehaviorFactory extends JComponentBehaviorFactory {
    @Override
    public JComponentHandlers jHandlers(ActorContext<TopLevelActorMessage> ctx, JCswServices cswServices) {
        return new JHcdComponentHandlers(ctx, cswServices);
    }
}
//#jcomponent-factory
