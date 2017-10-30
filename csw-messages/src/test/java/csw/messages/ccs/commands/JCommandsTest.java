package csw.messages.ccs.commands;

import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.generics.ParameterSetType;
import csw.messages.params.models.ObsId;
import csw.messages.params.models.Prefix;
import csw.messages.params.models.RunId;
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

    private final RunId runId = RunId.apply();
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

    @Test
    public void shouldAbleToCreateAndAccessSetupCommand() {
        Setup setup = new Setup(runId, obsId, prefix).add(encoderParam).add(epochStringParam);

        // runId, obsId, prefix, subsystem
        Assert.assertEquals(runId, setup.runId());
        Assert.assertEquals(obsId, setup.obsId());
        Assert.assertEquals(prefix, setup.prefixStr());
        Assert.assertEquals(new Prefix(prefix), setup.prefix());
        Assert.assertEquals(WFOS, setup.subsystem());

        // complete API
        assertOnCommandAPI(setup);
    }

    @Test
    public void shouldAbleToCreateAndAccessObserveCommand() {
        Observe observe = new Observe(runId, obsId, prefix).add(encoderParam).add(epochStringParam);

        // runId, obsId, prefix, subsystem
        Assert.assertEquals(runId, observe.runId());
        Assert.assertEquals(obsId, observe.obsId());
        Assert.assertEquals(prefix, observe.prefixStr());
        Assert.assertEquals(new Prefix(prefix), observe.prefix());
        Assert.assertEquals(WFOS, observe.subsystem());

        // complete API
        assertOnCommandAPI(observe);
    }

    @Test
    public void shouldAbleToCreateAndAccessWaitCommand() {
        Wait wait = new Wait(runId, obsId, prefix).add(encoderParam).add(epochStringParam);

        // runId, obsId, prefix, subsystem
        Assert.assertEquals(runId, wait.runId());
        Assert.assertEquals(obsId, wait.obsId());
        Assert.assertEquals(prefix, wait.prefixStr());
        Assert.assertEquals(new Prefix(prefix), wait.prefix());
        Assert.assertEquals(WFOS, wait.subsystem());

        // complete API
        assertOnCommandAPI(wait);
    }
}
