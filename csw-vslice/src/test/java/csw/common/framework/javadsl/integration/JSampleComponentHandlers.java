package csw.common.framework.javadsl.integration;


import akka.typed.ActorRef;
import akka.typed.javadsl.ActorContext;
import csw.common.ccs.Validation;
import csw.common.ccs.Validations;
import csw.common.components.SampleComponentState;
import csw.common.framework.javadsl.JComponentHandlers;
import csw.common.framework.models.CommandMsg;
import csw.common.framework.models.ComponentInfo;
import csw.common.framework.models.ComponentMsg;
import csw.common.framework.models.PubSub;
import csw.param.states.CurrentState;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletableFuture;

public class JSampleComponentHandlers extends JComponentHandlers<JComponentDomainMsg> {
    private ActorRef<PubSub.PublisherMsg<CurrentState>> pubSubRef;

    private CurrentState currentState = new CurrentState(SampleComponentState.prefix().prefix());

    JSampleComponentHandlers(ActorContext<ComponentMsg> ctx, ComponentInfo componentInfo, ActorRef<PubSub.PublisherMsg<CurrentState>> pubSubRef, Class<JComponentDomainMsg> klass) {
        super(ctx, componentInfo, pubSubRef, klass);
        this.pubSubRef = pubSubRef;
    }

    @Override
    public CompletableFuture<BoxedUnit> jInitialize() {
        return CompletableFuture.supplyAsync(() -> {
            CurrentState initState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.initChoice()));
            PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(initState);

            pubSubRef.tell(publish);
            return BoxedUnit.UNIT;
        });
    }

    @Override
    public void onRun() {
        CurrentState runState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.runChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(runState);

        pubSubRef.tell(publish);
    }

    @Override
    public void onDomainMsg(JComponentDomainMsg hcdDomainMsg) {
        CurrentState domainState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.domainChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(domainState);

        pubSubRef.tell(publish);
    }

    @Override
    public Validation onControlCommand(CommandMsg commandMsg) {

        CurrentState commandState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.commandChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(commandState);

        pubSubRef.tell(publish);
        return Validations.JValid();
    }

    @Override
    public void onShutdown() {
        CurrentState shutdownState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.shutdownChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(shutdownState);

        pubSubRef.tell(publish);
    }

    @Override
    public void onRestart() {
        CurrentState restartState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.restartChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(restartState);

        pubSubRef.tell(publish);
    }

    @Override
    public void onGoOffline() {
        CurrentState offlineState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.offlineChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(offlineState);

        pubSubRef.tell(publish);
    }

    @Override
    public void onGoOnline() {
        CurrentState onlineState = currentState.add(SampleComponentState.choiceKey().set(SampleComponentState.onlineChoice()));
        PubSub.Publish<CurrentState> publish = new PubSub.Publish<>(onlineState);

        pubSubRef.tell(publish);
    }
}
