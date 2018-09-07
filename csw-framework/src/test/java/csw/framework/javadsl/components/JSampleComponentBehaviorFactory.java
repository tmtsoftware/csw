package csw.framework.javadsl.components;

import akka.actor.typed.javadsl.ActorContext;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswServices;
import csw.messages.TopLevelActorMessage;

public class JSampleComponentBehaviorFactory extends JComponentBehaviorFactory {

    @Override
    public JComponentHandlers jHandlers(ActorContext<TopLevelActorMessage> ctx, JCswServices cswServices) {
        return new JSampleComponentHandlers(ctx, cswServices);
    }
}
