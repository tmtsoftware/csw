package csw.common.components.hcd;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.components.hcd.messages.HcdSampleMessages;
import csw.common.framework.javadsl.JHcdActor;
import csw.common.framework.javadsl.JHcdActorFactory;
import csw.common.framework.models.HcdComponentLifecycleMessage;
import csw.common.framework.models.HcdMsg;

public class JSampleHcdFactory extends JHcdActorFactory<HcdSampleMessages> {
    private final Class<HcdSampleMessages> klass;

    public JSampleHcdFactory(Class<HcdSampleMessages> klass) {
        super(klass);
        this.klass = klass;
    }

    @Override
    public JHcdActor<HcdSampleMessages> make(ActorContext<HcdMsg> ctx, ActorRef<HcdComponentLifecycleMessage> supervisor) {
        return new JSampleHcd(ctx, supervisor, klass);
    }
}
