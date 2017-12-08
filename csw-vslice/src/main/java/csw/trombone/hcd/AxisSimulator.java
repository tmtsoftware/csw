package csw.trombone.hcd;

import akka.typed.ActorRef;
import akka.typed.Behavior;
import akka.typed.javadsl.Actor;
import akka.typed.javadsl.Actor.MutableBehavior;
import akka.typed.javadsl.ActorContext;
import akka.typed.javadsl.ReceiveBuilder;

public class AxisSimulator extends MutableBehavior<SimulatorCommand> {
    private ActorContext<SimulatorCommand> context;
    private AxisConfig axisConfig;
    private ActorRef<AxisResponse> replyTo;

    private AxisSimulator(
            ActorContext<SimulatorCommand> ctx,
            AxisConfig axisConfig,
            ActorRef<AxisResponse> replyTo
    ) {
        this.context = ctx;
        this.axisConfig = axisConfig;
        this.replyTo = replyTo;
    }

    public static Behavior<AxisRequest> behavior(AxisConfig axisConfig, ActorRef<AxisResponse> replyTo) {
        return Actor.<SimulatorCommand>mutable(ctx -> new AxisSimulator(ctx, axisConfig, replyTo)).narrow();
    }

    @Override
    public Actor.Receive<SimulatorCommand> createReceive() {
        ReceiveBuilder<SimulatorCommand> receiveBuilder = receiveBuilder();
        return receiveBuilder.build();
    }


}
