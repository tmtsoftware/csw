package org.tmt.csw.sample;

import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.ComponentContext;
import csw.framework.models.JCswContext;

public class JSampleBehaviorFactory extends JComponentBehaviorFactory {
    @Override
    public JComponentHandlers jHandlers(ComponentContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        return new JSampleHandlers(ctx, cswCtx);
    }
}
