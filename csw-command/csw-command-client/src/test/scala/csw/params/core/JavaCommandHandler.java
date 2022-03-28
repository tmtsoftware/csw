/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import csw.params.commands.Observe;
import csw.params.commands.Setup;
import csw.params.commands.Wait;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.javadsl.JUnits;
import csw.prefix.models.Prefix;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.params.javadsl.JKeyType;
import csw.prefix.javadsl.JSubsystem;

import java.util.Set;

public abstract class JavaCommandHandler {

    private static final Prefix prefix = Prefix.apply(JSubsystem.WFOS, "red.detector");

    private static final Key<Integer> encoderIntKey = JKeyType.IntKey().make("encoder", JUnits.encoder);
    private static final Key<String> epochStringKey = JKeyType.StringKey().make("epoch", JUnits.year);

    static final Parameter<Integer> encoderParam = encoderIntKey.set(55, 66);
    static final Parameter<String> epochStringParam = epochStringKey.set("Event1", "Event2");

    private static final SystemEvent systemEvent = new SystemEvent(prefix, new EventName("")).add(encoderParam).add(epochStringParam);

    private JavaCommandHandler() {
    }

    public static Behavior<CommandMsg> behavior() {
        return Behaviors.receive(CommandMsg.class)
                .onMessage(CommandMsg.class, msg -> msg.command().getClass().isAssignableFrom(Setup.class), (msg) -> {
                    Set<Parameter<?>> jParamSet = ((Setup) msg.command()).jParamSet();
                    msg.ackTo().tell(jParamSet);
                    msg.replyTo().tell(systemEvent);
                    msg.obsIdAck().tell(msg.command().jMaybeObsId());
                    return Behaviors.same();
                })
                .onMessage(CommandMsg.class, msg -> msg.command().getClass().isAssignableFrom(Wait.class), (msg) -> {
                    Set<Parameter<?>> jParamSet = ((Setup) msg.command()).jParamSet();
                    msg.ackTo().tell(jParamSet);
                    msg.obsIdAck().tell(msg.command().jMaybeObsId());
                    return Behaviors.same();
                })
                .onMessage(CommandMsg.class, msg -> msg.command().getClass().isAssignableFrom(Observe.class), (msg) -> {
                    Set<Parameter<?>> jParamSet = ((Setup) msg.command()).jParamSet();
                    msg.ackTo().tell(jParamSet);
                    msg.obsIdAck().tell(msg.command().jMaybeObsId());
                    return Behaviors.same();
                })
                .build();
    }

}
