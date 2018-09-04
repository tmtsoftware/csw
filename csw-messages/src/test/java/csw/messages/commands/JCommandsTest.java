package csw.messages.commands;

import csw.messages.params.generics.JKeyType;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.generics.ParameterSetType;
import csw.messages.params.models.ObsId;
import csw.messages.params.models.Prefix;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static csw.messages.javadsl.JSubsystem.WFOS;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
// DEOPSCSW-320: Add command type in Setup, observe and wait
public class JCommandsTest {

    private final Key<Integer> encoderIntKey = JKeyType.IntKey().make("encoder");
    private final Key<String> epochStringKey = JKeyType.StringKey().make("epoch");
    private final Key<Integer> epochIntKey = JKeyType.IntKey().make("epoch");
    private final Key<Integer> notUsedKey = JKeyType.IntKey().make("notUsed");

    private final Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
    private final Parameter<String> epochStringParam = epochStringKey.set("A", "B");
    private final Parameter<Integer> epochIntParam = epochIntKey.set(44, 55);

    private final ObsId obsId = new ObsId("obsId");
    private final String prefix = "wfos.red.detector";
    private final CommandName commandName = new CommandName("move");

    private void assertOnCommandAPI(ParameterSetType<?> command) {
        // contains and exists
        Assert.assertFalse(command.contains(notUsedKey));
        Assert.assertTrue(command.contains(encoderIntKey));
        Assert.assertFalse(command.exists(epochIntKey));
        Assert.assertTrue(command.exists(epochStringKey));

        // jFind
        Assert.assertEquals(epochStringParam, command.jFind(epochStringParam).get());
        Assert.assertEquals(Optional.empty(), command.jFind(epochIntParam));

        // jGet
        Assert.assertEquals(epochStringParam, command.jGet(epochStringKey).get());
        Assert.assertEquals(Optional.empty(), command.jGet(epochIntKey));
        Assert.assertEquals(epochStringParam, command.jGet(epochStringKey.keyName(), epochStringKey.keyType()).get());
        Assert.assertEquals(Optional.empty(), command.jGet(epochIntKey.keyName(), epochIntKey.keyType()));

        // size
        Assert.assertEquals(2, command.size());

        // jParamSet
        HashSet<Parameter<?>> expectedParamSet = new HashSet<>(Arrays.asList(encoderParam, epochStringParam));
        Assert.assertEquals(expectedParamSet, command.jParamSet());

        // parameter
        Assert.assertEquals(epochStringParam, command.parameter(epochStringKey));

        // jMissingKeys
        HashSet<String> expectedMissingKeys = new HashSet<>(Collections.singletonList(notUsedKey.keyName()));
        Set<String> jMissingKeys = command.jMissingKeys(encoderIntKey, epochStringKey, notUsedKey);
        Assert.assertEquals(expectedMissingKeys, jMissingKeys);

        // getStringMap
        List<String> encoderStringParam = encoderParam.jValues().stream().map(Object::toString)
                .collect(Collectors.toList());
        Map<String, String> expectedParamMap = new LinkedHashMap<String, String>() {
            {
                put(encoderParam.keyName(),  String.join(",", encoderStringParam));
                put(epochStringParam.keyName(),  String.join(",", epochStringParam.jValues()));
            }
        };
        Assert.assertEquals(expectedParamMap, command.jGetStringMap());
    }

    // DEOPSCSW-315: Make ObsID optional in commands
    // DEOPSCSW-369: Unique runId for commands
    @Test
    public void shouldAbleToCreateAndAccessSetupCommand() {
        Setup setup = new Setup(new Prefix(prefix), commandName, Optional.of(obsId)).add(encoderParam).add(epochStringParam);

        // runId, obsId, prefix, subsystem
        Assert.assertNotNull(setup.runId());
        Assert.assertEquals(Optional.of(obsId), setup.jMaybeObsId());
        Assert.assertEquals(prefix, setup.source().prefix());
        Assert.assertEquals(WFOS, setup.source().subsystem());
        Assert.assertEquals(commandName, setup.commandName());

        // complete API
        assertOnCommandAPI(setup);

        Setup mutatedSetup1 = setup.add(epochIntParam);
        Assert.assertNotEquals(setup.runId(), mutatedSetup1.runId());
        Assert.assertEquals(setup.source(), mutatedSetup1.source());
        Assert.assertEquals(setup.commandName(), mutatedSetup1.commandName());
        Assert.assertEquals(setup.jMaybeObsId(), mutatedSetup1.jMaybeObsId());

        Setup mutatedSetup2 = mutatedSetup1.remove(epochIntParam);
        Assert.assertNotEquals(mutatedSetup2.runId(), mutatedSetup1.runId());
        Assert.assertEquals(mutatedSetup2.source(), mutatedSetup1.source());
        Assert.assertEquals(mutatedSetup2.commandName(), mutatedSetup1.commandName());
        Assert.assertEquals(mutatedSetup2.jMaybeObsId(), mutatedSetup1.jMaybeObsId());

    }

    // DEOPSCSW-315: Make ObsID optional in commands
    // DEOPSCSW-369: Unique runId for commands
    @Test
    public void shouldAbleToCreateAndAccessObserveCommand() {
        Observe observe = new Observe(new Prefix(prefix), commandName, Optional.empty()).add(encoderParam).add(epochStringParam);

        // runId, prefix, obsId, subsystem
        Assert.assertNotNull(observe.runId());
        Assert.assertEquals(Optional.empty(), observe.jMaybeObsId());
        Assert.assertEquals(prefix, observe.source().prefix());
        Assert.assertEquals(commandName, observe.commandName());
        Assert.assertEquals(WFOS, observe.source().subsystem());


        // complete API
        assertOnCommandAPI(observe);

        Observe mutatedSetup1 = observe.add(epochIntParam);
        Assert.assertNotEquals(observe.runId(), mutatedSetup1.runId());
        Assert.assertEquals(observe.source(), mutatedSetup1.source());
        Assert.assertEquals(observe.commandName(), mutatedSetup1.commandName());
        Assert.assertEquals(observe.jMaybeObsId(), mutatedSetup1.jMaybeObsId());

        Observe mutatedSetup2 = mutatedSetup1.remove(epochIntParam);
        Assert.assertNotEquals(mutatedSetup2.runId(), mutatedSetup1.runId());
        Assert.assertEquals(mutatedSetup2.source(), mutatedSetup1.source());
        Assert.assertEquals(mutatedSetup2.commandName(), mutatedSetup1.commandName());
        Assert.assertEquals(mutatedSetup2.jMaybeObsId(), mutatedSetup1.jMaybeObsId());
    }

    // DEOPSCSW-369: Unique runId for commands
    @Test
    public void shouldAbleToCreateAndAccessWaitCommand() {
        Wait wait = new Wait(new Prefix(prefix), commandName, Optional.of(obsId)).add(encoderParam).add(epochStringParam);

        // runId, obsId, prefix, subsystem
        Assert.assertNotNull(wait.runId());
        Assert.assertEquals(obsId, wait.jMaybeObsId().get());
        Assert.assertEquals(prefix, wait.source().prefix());
        Assert.assertEquals(commandName, wait.commandName());
        Assert.assertEquals(WFOS, wait.source().subsystem());


        // complete API
        assertOnCommandAPI(wait);

        Wait mutatedSetup1 = wait.add(epochIntParam);
        Assert.assertNotEquals(wait.runId(), mutatedSetup1.runId());
        Assert.assertEquals(wait.source(), mutatedSetup1.source());
        Assert.assertEquals(wait.commandName(), mutatedSetup1.commandName());
        Assert.assertEquals(wait.jMaybeObsId(), mutatedSetup1.jMaybeObsId());

        Wait mutatedSetup2 = mutatedSetup1.remove(epochIntParam);
        Assert.assertNotEquals(mutatedSetup2.runId(), mutatedSetup1.runId());
        Assert.assertEquals(mutatedSetup2.source(), mutatedSetup1.source());
        Assert.assertEquals(mutatedSetup2.commandName(), mutatedSetup1.commandName());
        Assert.assertEquals(mutatedSetup2.jMaybeObsId(), mutatedSetup1.jMaybeObsId());
    }

    @Test
    public void shoulRdAbleToCloneAnExistingCommand() {
        Setup setup = new Setup(new Prefix(prefix), commandName, Optional.of(obsId)).add(encoderParam).add(epochStringParam);
        Setup setup2 = setup.cloneCommand();
        Assert.assertNotEquals(setup.runId(), setup2.runId());
        Assert.assertEquals(setup.commandName(), setup2.commandName());
        Assert.assertEquals(setup.jMaybeObsId(), setup2.jMaybeObsId());
        Assert.assertEquals(setup.jParamSet(), setup2.jParamSet());
        Assert.assertEquals(setup.source(), setup2.source());

        Observe observe = new Observe(new Prefix(prefix), commandName, Optional.empty()).add(encoderParam).add(epochStringParam);
        Observe observe2 = observe.cloneCommand();
        Assert.assertNotEquals(observe.runId(), observe2.runId());
        Assert.assertEquals(observe.commandName(), observe2.commandName());
        Assert.assertEquals(observe.jMaybeObsId(), observe2.jMaybeObsId());
        Assert.assertEquals(observe.jParamSet(), observe2.jParamSet());
        Assert.assertEquals(observe.source(), observe2.source());

        Wait wait = new Wait(new Prefix(prefix), commandName, Optional.of(obsId)).add(encoderParam).add(epochStringParam);
        Wait wait2 = wait.cloneCommand();
        Assert.assertNotEquals(wait.runId(), wait2.runId());
        Assert.assertEquals(wait.commandName(), wait2.commandName());
        Assert.assertEquals(wait.jMaybeObsId(), wait2.jMaybeObsId());
        Assert.assertEquals(wait.jParamSet(), wait2.jParamSet());
        Assert.assertEquals(wait.source(), wait2.source());
    }
}
