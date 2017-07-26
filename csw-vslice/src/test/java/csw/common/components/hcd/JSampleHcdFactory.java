package csw.common.components.hcd;

import akka.typed.javadsl.ActorContext;
import csw.common.components.hcd.messages.JHcdDomainMessages;
import csw.common.framework.javadsl.hcd.JHcdHandlers;
import csw.common.framework.javadsl.hcd.JHcdHandlersFactory;
import csw.common.framework.models.HcdMsg;

public class JSampleHcdFactory extends JHcdHandlersFactory<JHcdDomainMessages> {
    private final Class<JHcdDomainMessages> klass;

    public JSampleHcdFactory(Class<JHcdDomainMessages> klass) {
        super(klass);
        this.klass = klass;
    }

    @Override
    public JHcdHandlers<JHcdDomainMessages> make(ActorContext<HcdMsg> ctx) {
        return new JSampleHcdHandlers(ctx, klass);
    }
}
