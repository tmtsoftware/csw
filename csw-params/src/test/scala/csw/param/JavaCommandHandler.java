package csw.param;

import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import csw.param.commands.Observe;
import csw.param.commands.Setup;
import csw.param.commands.Wait;
import csw.param.events.StatusEvent;
import csw.param.generics.JKeyTypes;
import csw.param.generics.Key;
import csw.param.generics.Parameter;

import java.util.Set;

public abstract class JavaCommandHandler {

    private static final String prefix = "wfos.red.detector";

    private static final Key<Integer> encoderIntKey = JKeyTypes.IntKey().make("encoder");
    private static final Key<String> epochStringKey = JKeyTypes.StringKey().make("epoch");

    static final Parameter<Integer> encoderParam = encoderIntKey.set(55, 66);
    static final Parameter<String> epochStringParam = epochStringKey.set("Event1", "Event2");

    private static StatusEvent statusEvent = new StatusEvent(prefix).add(encoderParam).add(epochStringParam);

    private JavaCommandHandler() {
    }

    public static Behavior<CommandMsg> behavior() {
        return Actor.immutable(CommandMsg.class)
                .onMessage(CommandMsg.class, msg -> msg.command().getClass().isAssignableFrom(Setup.class),(ctx, msg) -> {
                    Set<Parameter<?>> jParamSet = ((Setup) msg.command()).jParamSet();
                    msg.ackTo().tell(jParamSet);
                    msg.replyTo().tell(statusEvent);
                    return Actor.same();
                })
                .onMessage(CommandMsg.class, msg -> msg.command().getClass().isAssignableFrom(Wait.class),(ctx, msg) -> {
                    Set<Parameter<?>> jParamSet = ((Setup) msg.command()).jParamSet();
                    msg.ackTo().tell(jParamSet);
                    return Actor.same();
                })
                .onMessage(CommandMsg.class, msg -> msg.command().getClass().isAssignableFrom(Observe.class),(ctx, msg) -> {
                    Set<Parameter<?>> jParamSet = ((Setup) msg.command()).jParamSet();
                    msg.ackTo().tell(jParamSet);
                    return Actor.same();
                })
                .build();
    }

}


