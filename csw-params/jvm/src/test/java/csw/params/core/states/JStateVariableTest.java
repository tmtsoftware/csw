/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.states;

import csw.params.commands.CommandName;
import csw.params.commands.Setup;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.ObsId;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.models.Prefix;
import csw.prefix.javadsl.JSubsystem;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
public class JStateVariableTest {

    private final Key<Integer> encoderIntKey = JKeyType.IntKey().make("encoder", JUnits.encoder);
    private final Key<String> epochStringKey = JKeyType.StringKey().make("epoch", JUnits.year);
    private final Key<Integer> epochIntKey = JKeyType.IntKey().make("epoch", JUnits.year);

    private final Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
    private final Parameter<String> epochStringParam = epochStringKey.set("A", "B");

    private final Prefix prefix = Prefix.apply(JSubsystem.WFOS, "red.detector");

    @Test
    public void shouldAbleToCreateCurrentState__DEOPSCSW_183_DEOPSCSW_185() {
        CurrentState currentState = new CurrentState(prefix, new StateName("testStateName")).add(encoderParam).add(epochStringParam);

        // typeName and prefix
        Assert.assertEquals(CurrentState.class.getSimpleName(), currentState.typeName());
        Assert.assertEquals(prefix, currentState.prefix());

        // exists
        Assert.assertTrue(currentState.exists(epochStringKey));
        Assert.assertFalse(currentState.exists(epochIntKey));

        // jParamSet
        Set<Parameter<?>> expectedParamSet = Set.of(encoderParam, epochStringParam);
        Assert.assertEquals(expectedParamSet, currentState.jParamSet());
    }

    @Test
    public void shouldAbleToCreateCurrentStateFromSetup__DEOPSCSW_183_DEOPSCSW_185() {
        Prefix source = prefix;
        Setup setup = new Setup(source, new CommandName("move"), Optional.of(ObsId.apply("2020A-001-123"))).add(encoderParam).add(epochStringParam);
        CurrentState currentState = new CurrentState(new StateName("testStateName"), setup);

        // typeName and prefix
        Assert.assertEquals(CurrentState.class.getSimpleName(), currentState.typeName());
        Assert.assertEquals(source, currentState.prefix());

        // exists
        Assert.assertTrue(currentState.exists(epochStringKey));
        Assert.assertFalse(currentState.exists(epochIntKey));

        // jParamSet
        Set<Parameter<?>> expectedParamSet = Set.of(encoderParam, epochStringParam);
        Assert.assertEquals(expectedParamSet, currentState.jParamSet());
    }

    @Test
    public void shouldAbleToCreateDemandState__DEOPSCSW_183_DEOPSCSW_185() {
        DemandState demandState = new DemandState(prefix, new StateName("testStateName")).add(encoderParam).add(epochStringParam);

        // typeName and prefix
        Assert.assertEquals(DemandState.class.getSimpleName(), demandState.typeName());
        Assert.assertEquals(prefix, demandState.prefix());

        // exists
        Assert.assertTrue(demandState.exists(epochStringKey));
        Assert.assertFalse(demandState.exists(epochIntKey));

        // jParamSet
        Set<Parameter<?>> expectedParamSet = Set.of(encoderParam, epochStringParam);
        Assert.assertEquals(expectedParamSet, demandState.jParamSet());
    }

    @Test
    public void shouldAbleToCreateDemandStateFromSetup__DEOPSCSW_183_DEOPSCSW_185() {
        Prefix source = prefix;
        Setup setup = new Setup(source, new CommandName("move"), Optional.of(ObsId.apply("2020A-001-123"))).add(encoderParam).add(epochStringParam);
        DemandState demandState = new DemandState(new StateName("testStateName"), setup);

        // typeName and prefix
        Assert.assertEquals(DemandState.class.getSimpleName(), demandState.typeName());
        Assert.assertEquals(source, demandState.prefix());

        // exists
        Assert.assertTrue(demandState.exists(epochStringKey));
        Assert.assertFalse(demandState.exists(epochIntKey));

        // jParamSet
        Set<Parameter<?>> expectedParamSet = Set.of(encoderParam, epochStringParam);
        Assert.assertEquals(expectedParamSet, demandState.jParamSet());
    }

    @Test
    public void shouldAbleToMatchWithDefaultMatcher__DEOPSCSW_183_DEOPSCSW_185() {
        CurrentState currentState = new CurrentState(prefix, new StateName("testStateName")).add(encoderParam).add(epochStringParam);
        DemandState demandState = new DemandState(prefix, new StateName("testStateName")).add(encoderParam).add(epochStringParam);

        Assert.assertTrue(StateVariable.defaultMatcher(demandState, currentState));
    }

    @Test
    public void shouldAbleToCreateCurrentStatesUsingVargs__DEOPSCSW_183_DEOPSCSW_185() {
        CurrentState currentState1 = new CurrentState(prefix, new StateName("testStateName")).add(encoderParam);
        CurrentState currentState2 = new CurrentState(prefix, new StateName("testStateName")).add(epochStringParam);
        CurrentState currentState3 = new CurrentState(prefix, new StateName("testStateName")).add(epochStringParam);
        List<CurrentState> expectedCurrentStates = Arrays.asList(currentState1, currentState2, currentState3);

        CurrentStates currentStates = StateVariable.createCurrentStates(currentState1, currentState2, currentState3);

        List<CurrentState> actualCurrentStates = currentStates.jStates();
        Assert.assertEquals(expectedCurrentStates, actualCurrentStates);
    }

    @Test
    public void shouldAbleToCreateCurrentStatesUsingList__DEOPSCSW_183_DEOPSCSW_185() {
        CurrentState currentState1 = new CurrentState(prefix, new StateName("testStateName")).add(encoderParam);
        CurrentState currentState2 = new CurrentState(prefix, new StateName("testStateName")).add(epochStringParam);
        CurrentState currentState3 = new CurrentState(prefix, new StateName("testStateName")).add(epochStringParam);
        List<CurrentState> expectedCurrentStates = Arrays.asList(currentState1, currentState2, currentState3);

        CurrentStates currentStates = StateVariable.createCurrentStates(expectedCurrentStates);

        List<CurrentState> actualCurrentStates = currentStates.jStates();
        Assert.assertEquals(expectedCurrentStates, actualCurrentStates);
    }
}
