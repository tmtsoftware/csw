package csw.param.commands;

import csw.param.Subsystem;
import csw.param.models.Prefix;
import csw.param.parameters.JKeyTypes;
import csw.param.parameters.Key;
import csw.param.parameters.Parameter;
import csw.param.parameters.ParameterSetType;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class CommandTest {

    Key<Integer> encoderIntKey = JKeyTypes.IntKey().make("encoder");
    Key<String> epochStringKey = JKeyTypes.StringKey().make("epoch");
    Key<Integer> epochIntKey = JKeyTypes.IntKey().make("epoch");
    Key<Integer> notUsedKey = JKeyTypes.IntKey().make("notUsed");

    Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
    Parameter<String> epochStringParam = epochStringKey.set("A");
    Parameter<Integer> epochIntParam = epochIntKey.set(44,55);

    CommandInfo commandInfo = new CommandInfo("obsId");
    String prefix = "wfos.red.detector";
    
    public void assertOnCommandAPI(ParameterSetType<?> command) {
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
        Setup setup = new Setup(commandInfo, prefix).add(encoderParam).add(epochStringParam);

        // commandInfo, prefix, subsystem
        Assert.assertEquals(commandInfo, setup.info());
        Assert.assertEquals(prefix, setup.prefixStr());
        Assert.assertEquals(new Prefix(prefix), setup.prefix());
        Assert.assertEquals(Subsystem.WFOS$.MODULE$, setup.subsystem());

        // complete API
        assertOnCommandAPI(setup);
    }

    @Test
    public void shouldAbleToCreateAndAccessObserveCommand() {
        Observe observe = new Observe(commandInfo, prefix).add(encoderParam).add(epochStringParam);

        // commandInfo, prefix, subsystem
        Assert.assertEquals(commandInfo, observe.info());
        Assert.assertEquals(prefix, observe.prefixStr());
        Assert.assertEquals(new Prefix(prefix), observe.prefix());
        Assert.assertEquals(Subsystem.WFOS$.MODULE$, observe.subsystem());

        // complete API
        assertOnCommandAPI(observe);
    }

    @Test
    public void shouldAbleToCreateAndAccessWaitCommand() {
        Wait wait = new Wait(commandInfo, prefix).add(encoderParam).add(epochStringParam);

        // commandInfo, prefix, subsystem
        Assert.assertEquals(commandInfo, wait.info());
        Assert.assertEquals(prefix, wait.prefixStr());
        Assert.assertEquals(new Prefix(prefix), wait.prefix());
        Assert.assertEquals(Subsystem.WFOS$.MODULE$, wait.subsystem());

        // complete API
        assertOnCommandAPI(wait);
    }
}
