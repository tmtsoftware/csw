package csw.messages.ccs.commands;

import csw.messages.params.generics.JKeyTypes;
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
public class JCommandsTest {

    private final Key<Integer> encoderIntKey = JKeyTypes.IntKey().make("encoder");
    private final Key<String> epochStringKey = JKeyTypes.StringKey().make("epoch");
    private final Key<Integer> epochIntKey = JKeyTypes.IntKey().make("epoch");
    private final Key<Integer> notUsedKey = JKeyTypes.IntKey().make("notUsed");

    private final Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
    private final Parameter<String> epochStringParam = epochStringKey.set("A", "B");
    private final Parameter<Integer> epochIntParam = epochIntKey.set(44, 55);

    private final ObsId obsId = new ObsId("obsId");
    private final String prefix = "wfos.red.detector";

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
    @Test
    public void shouldAbleToCreateAndAccessSetupCommand() {
        Setup setup = new Setup(prefix, prefix, Optional.of(obsId)).add(encoderParam).add(epochStringParam);

        // runId, obsId, prefix, subsystem
        Assert.assertNotNull(setup.runId());
        Assert.assertEquals(Optional.of(obsId), setup.jMaybeObsId());
        Assert.assertEquals(prefix, setup.target().prefix());
        Assert.assertEquals(new Prefix(prefix), setup.target());
        Assert.assertEquals(WFOS, setup.target().subsystem());

        // complete API
        assertOnCommandAPI(setup);
    }

    // DEOPSCSW-315: Make ObsID optional in commands
    @Test
    public void shouldAbleToCreateAndAccessObserveCommand() {
        Observe observe = new Observe(prefix, prefix, Optional.empty()).add(encoderParam).add(epochStringParam);

        // runId, prefix, obsId, subsystem
        Assert.assertNotNull(observe.runId());
        Assert.assertEquals(Optional.empty(), observe.jMaybeObsId());
        Assert.assertEquals(prefix, observe.target().prefix());
        Assert.assertEquals(new Prefix(prefix), observe.target());
        Assert.assertEquals(WFOS, observe.target().subsystem());

        // complete API
        assertOnCommandAPI(observe);
    }

    @Test
    public void shouldAbleToCreateAndAccessWaitCommand() {
        Wait wait = new Wait(prefix, prefix, Optional.of(obsId)).add(encoderParam).add(epochStringParam);

        // runId, obsId, prefix, subsystem
        Assert.assertNotNull(wait.runId());
        Assert.assertEquals(obsId, wait.jMaybeObsId().get());
        Assert.assertEquals(prefix, wait.target().prefix());
        Assert.assertEquals(new Prefix(prefix), wait.target());
        Assert.assertEquals(WFOS, wait.target().subsystem());

        // complete API
        assertOnCommandAPI(wait);
    }
}
