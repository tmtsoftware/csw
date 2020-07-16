package csw.framework.javadsl.components;

import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.ComponentContext;
import csw.framework.models.JCswContext;
import csw.command.client.messages.TopLevelActorMessage;

public class JSampleComponentBehaviorFactory extends JComponentBehaviorFactory {

    @Override
    public JComponentHandlers jHandlers(ComponentContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        return new JSampleComponentHandlers(ctx, cswCtx);
    }
}
