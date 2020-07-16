package example.framework.components.hcd;

import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.ComponentContext;
import csw.framework.models.JCswContext;
import csw.command.client.messages.TopLevelActorMessage;

//#jcomponent-factory
public class JHcdComponentBehaviorFactory extends JComponentBehaviorFactory {
    @Override
    public JComponentHandlers jHandlers(ComponentContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        return new JHcdComponentHandlers(ctx, cswCtx);
    }
}
//#jcomponent-factory
