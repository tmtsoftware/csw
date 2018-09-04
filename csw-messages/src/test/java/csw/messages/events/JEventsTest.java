package csw.messages.events;

import csw.messages.params.generics.JKeyType;
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
// DEOPSCSW-327: Define Event Data Structure
// DEOPSCSW-328: Basic information of Event needed for routing and Diagnostic use
// DEOPSCSW-329: Providing Mandatory information during Event Creation
public class JEventsTest {
    private final Key<Integer> encoderIntKey = JKeyType.IntKey().make("encoder");
    private final Key<String> epochStringKey = JKeyType.StringKey().make("epoch");
    private final Key<Integer> epochIntKey = JKeyType.IntKey().make("epoch");
    private final Key<java.lang.Byte> epochByteKey = JKeyType.ByteKey().make("epoch");
    private final Key<Integer> notUsedKey = JKeyType.IntKey().make("notUsed");

    private final Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
    private final Parameter<String> epochStringParam = epochStringKey.set("A", "B");
    private final Parameter<Integer> epochIntParam = epochIntKey.set(44, 55);
    private final Parameter<Byte> epochByteParam = epochByteKey.set(new Byte[]{10, 20});

    private final Prefix prefix = new Prefix("wfos.red.detector");

    private<T extends ParameterSetType & Event> void assertOnEventsAPI(T event) {

        // metadata (eventId, source, eventName, eventTime)
        EventName name = new EventName("filter wheel");
        Assert.assertNotNull(event.eventId());
        Assert.assertEquals(prefix, event.source());
        Assert.assertEquals(name, event.eventName());
        Assert.assertNotNull(event.eventTime());
        Assert.assertEquals(event.eventKey().toString(), prefix.prefix() + "." + name.name());

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
    public void shouldAbleToCreateAndAccessSystemEvent() {
        SystemEvent systemEvent = new SystemEvent(prefix, new EventName("filter wheel")).add(encoderParam).add(epochStringParam);
        assertOnEventsAPI(systemEvent);
    }

    @Test
    public void shouldAbleToCreateAndAccessObserveEvent() {
        ObserveEvent observeEvent = new ObserveEvent(prefix, new EventName("filter wheel")).add(encoderParam).add(epochStringParam);
        assertOnEventsAPI(observeEvent);
    }

    @Test
    public void shouldAbleToRemoveParamsInSystemEvent() {
        SystemEvent systemEvent = new SystemEvent(prefix, new EventName("filter wheel")).add(encoderParam).add(epochByteParam);
        Assert.assertEquals(2, systemEvent.size());
        Assert.assertArrayEquals(new Byte[]{10, 20}, systemEvent.jGet(epochByteKey).get().jValues().toArray());
        SystemEvent mutatedEvent = systemEvent.remove(encoderParam);
        Assert.assertEquals(1, mutatedEvent.size());
    }

    @Test
    public void shouldAbleToRemoveParamsInObserveEvent() {
        ObserveEvent observeEvent = new ObserveEvent(prefix, new EventName("filter wheel")).add(encoderParam).add(epochByteParam);
        Assert.assertEquals(2, observeEvent.size());
        Assert.assertArrayEquals(new Byte[]{10, 20}, observeEvent.jGet(epochByteKey).get().jValues().toArray());
        ObserveEvent mutatedEvent = observeEvent.remove(encoderParam);
        Assert.assertEquals(1, mutatedEvent.size());
    }

    @Test
    public void shouldBeUniqueWhenParametersAreAddedOrRemovedForSystem() {
        SystemEvent systemEvent1 = new SystemEvent(prefix, new EventName("filter wheel")).add(encoderParam).add(epochStringParam);
        SystemEvent systemEvent2 = systemEvent1.add(epochIntParam);
        SystemEvent systemEvent3 = systemEvent2.remove(epochIntKey);

        assertOnEventsAPI(systemEvent1);
        Assert.assertNotEquals(systemEvent1.eventId(), systemEvent2.eventId());
        Assert.assertEquals(systemEvent1.eventName(), systemEvent2.eventName());
        Assert.assertEquals(systemEvent1.source(), systemEvent2.source());

        Assert.assertNotEquals(systemEvent3.eventId(), systemEvent2.eventId());
        Assert.assertEquals(systemEvent3.eventName(), systemEvent2.eventName());
        Assert.assertEquals(systemEvent3.source(), systemEvent2.source());
    }

    @Test
    public void shouldBeUniqueIdWhenParametersAreAddedOrRemovedForObserve() {
        ObserveEvent observeEvent1 = new ObserveEvent(prefix, new EventName("filter wheel")).add(encoderParam).add(epochStringParam);
        ObserveEvent observeEvent2 = observeEvent1.add(epochIntParam);
        ObserveEvent observeEvent3 = observeEvent2.remove(epochIntKey);

        assertOnEventsAPI(observeEvent1);
        Assert.assertNotEquals(observeEvent1.eventId(), observeEvent2.eventId());
        Assert.assertEquals(observeEvent1.eventName(), observeEvent2.eventName());
        Assert.assertEquals(observeEvent1.source(), observeEvent2.source());

        Assert.assertNotEquals(observeEvent3.eventId(), observeEvent2.eventId());
        Assert.assertEquals(observeEvent3.eventName(), observeEvent2.eventName());
        Assert.assertEquals(observeEvent3.source(), observeEvent2.source());

    }
}
