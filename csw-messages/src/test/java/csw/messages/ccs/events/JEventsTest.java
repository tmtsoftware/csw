package csw.messages.ccs.events;

import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.generics.ParameterSetType;
import csw.messages.params.models.Prefix;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
public class JEventsTest {
    private final Key<Integer> encoderIntKey = JKeyTypes.IntKey().make("encoder");
    private final Key<String> epochStringKey = JKeyTypes.StringKey().make("epoch");
    private final Key<Integer> epochIntKey = JKeyTypes.IntKey().make("epoch");
    private final Key<Integer> notUsedKey = JKeyTypes.IntKey().make("notUsed");

    private final Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
    private final Parameter<String> epochStringParam = epochStringKey.set("A", "B");
    private final Parameter<Integer> epochIntParam = epochIntKey.set(44, 55);

    private final Prefix prefix = new Prefix("wfos.red.detector");

    private<T extends ParameterSetType & Event> void assertOnEventsAPI(T event) {
        // eventInfo, prefix, subsystem, source
        Assert.assertEquals(prefix, event.source());

        // contains and exists
        Assert.assertFalse(event.contains(notUsedKey));
        Assert.assertTrue(event.contains(encoderIntKey));
        Assert.assertFalse(event.exists(epochIntKey));
        Assert.assertTrue(event.exists(epochStringKey));

        // jFind
        Assert.assertEquals(epochStringParam, event.jFind(epochStringParam).get());
        Assert.assertEquals(Optional.empty(), event.jFind(epochIntParam));

        // jGet
        Assert.assertEquals(epochStringParam, event.jGet(epochStringKey).get());
        Assert.assertEquals(Optional.empty(), event.jGet(epochIntKey));
        Assert.assertEquals(epochStringParam, event.jGet(epochStringKey.keyName(), epochStringKey.keyType()).get());
        Assert.assertEquals(Optional.empty(), event.jGet(epochIntKey.keyName(), epochIntKey.keyType()));

        // size
        Assert.assertEquals(2, event.size());

        // jParamSet
        HashSet<Parameter<?>> expectedParamSet = new HashSet<>(Arrays.asList(encoderParam, epochStringParam));
        Assert.assertEquals(expectedParamSet, event.jParamSet());

        // parameter
        Assert.assertEquals(epochStringParam, event.parameter(epochStringKey));

        // jMissingKeys
        HashSet<String> expectedMissingKeys = new HashSet<>(Collections.singletonList(notUsedKey.keyName()));
        Set<String> jMissingKeys = event.jMissingKeys(encoderIntKey, epochStringKey, notUsedKey);
        Assert.assertEquals(expectedMissingKeys, jMissingKeys);

        // getStringMap
        List<String> encoderStringParam = encoderParam.jValues().stream().map(Object::toString)
                .collect(Collectors.toList());
        Map<String, String> expectedParamMap = new LinkedHashMap<String, String>() {
            {
                put(encoderParam.keyName(), String.join(",", encoderStringParam));
                put(epochStringParam.keyName(), String.join(",", epochStringParam.jValues()));
            }
        };
        Assert.assertEquals(expectedParamMap, event.jGetStringMap());
    }

    @Test
    public void shouldAbleToCreateAndAccessObserveEvent() {
        ObserveEvent observeEvent = new ObserveEvent(prefix, "filter wheel").add(encoderParam).add(epochStringParam);
        assertOnEventsAPI(observeEvent);
    }

    @Test
    public void shouldAbleToCreateAndAccessSystemEvent() {
        SystemEvent systemEvent = new SystemEvent(prefix, "filter wheel").add(encoderParam).add(epochStringParam);
        assertOnEventsAPI(systemEvent);
    }

    @Test
    public void shouldAbleToCreateAndAccessSystemEventWithCustomInfo() {
        SystemEvent systemEvent = new SystemEvent(prefix, "filter wheel").add(encoderParam).add(epochStringParam);
        Assert.assertEquals(prefix, systemEvent.source());
        Assert.assertEquals("filter wheel", systemEvent.name());
        Assert.assertNotNull(systemEvent.eventId());
        Assert.assertNotNull(systemEvent.eventTime());
    }

}
