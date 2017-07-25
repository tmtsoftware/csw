package csw.common.components.hcd;

import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.components.hcd.messages.JHcdDomainMessages;
import csw.common.framework.javadsl.hcd.JHcdActor;
import csw.common.framework.javadsl.hcd.JHcdActorFactory;
import csw.common.framework.models.HcdResponseMode;
import csw.common.framework.models.HcdMsg;

public class JSampleHcdFactory extends JHcdActorFactory<JHcdDomainMessages> {
    private final Class<JHcdDomainMessages> klass;

    public JSampleHcdFactory(Class<JHcdDomainMessages> klass) {
        super(klass);
        this.klass = klass;
    }

    @Override
    public JHcdActor<JHcdDomainMessages> make(ActorContext<HcdMsg> ctx, ActorRef<HcdResponseMode> supervisor) {
        return new JSampleHcd(ctx, supervisor, klass);
    }
}
