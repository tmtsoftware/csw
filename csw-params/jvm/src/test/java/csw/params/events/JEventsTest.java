package csw.params.events;

import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.generics.ParameterSetType;
import csw.params.core.models.ObsId;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.models.Prefix;
import csw.prefix.javadsl.JSubsystem;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.*;
import java.util.stream.Collectors;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
// DEOPSCSW-327: Define Event Data Structure
// DEOPSCSW-328: Basic information of Event needed for routing and Diagnostic use
// DEOPSCSW-329: Providing Mandatory information during Event Creation
public class JEventsTest extends JUnitSuite {
    private final Key<Integer> encoderIntKey = JKeyType.IntKey().make("encoder", JUnits.encoder);
    private final Key<String> epochStringKey = JKeyType.StringKey().make("epoch", JUnits.year);
    private final Key<Integer> epochIntKey = JKeyType.IntKey().make("epoch", JUnits.year);
    private final Key<Byte> epochByteKey = JKeyType.ByteKey().make("epoch", JUnits.year);
    private final Key<Integer> notUsedKey = JKeyType.IntKey().make("notUsed", JUnits.NoUnits);

    private final Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
    private final Parameter<String> epochStringParam = epochStringKey.set("A", "B");
    private final Parameter<Integer> epochIntParam = epochIntKey.set(44, 55);
    private final Parameter<Byte> epochByteParam = epochByteKey.setAll(new Byte[]{10, 20});

    private final Prefix prefix = Prefix.apply(JSubsystem.WFOS, "red.detector");

    private <T extends ParameterSetType & Event> void assertOnEventsAPI(T event, EventName name) {

        // metadata (eventId, source, eventName, eventTime)
        Assert.assertNotNull(event.eventId());
        Assert.assertEquals(prefix, event.source());
        Assert.assertEquals(name, event.eventName());
        Assert.assertNotNull(event.eventTime());
        Assert.assertEquals(event.eventKey().toString(), prefix + "." + name.name());

        // contains and exists
        Assert.assertFalse(event.contains(notUsedKey));
        Assert.assertTrue(event.contains(encoderIntKey));
        Assert.assertFalse(event.exists(epochIntKey));
        Assert.assertTrue(event.exists(epochStringKey));

        // jFind
        Assert.assertEquals(epochStringParam, event.jFind(epochStringParam).orElseThrow());
        Assert.assertEquals(Optional.empty(), event.jFind(epochIntParam));

        // jGet
        Assert.assertEquals(epochStringParam, event.jGet(epochStringKey).orElseThrow());
        Assert.assertEquals(Optional.empty(), event.jGet(epochIntKey));
        Assert.assertEquals(epochStringParam, event.jGet(epochStringKey.keyName(), epochStringKey.keyType()).orElseThrow());
        Assert.assertEquals(Optional.empty(), event.jGet(epochIntKey.keyName(), epochIntKey.keyType()));

        // size
        Assert.assertEquals(2, event.size());

        // jParamSet
        var expectedParamSet = Set.of(encoderParam, epochStringParam);
        Assert.assertEquals(expectedParamSet, event.jParamSet());

        // parameter
        Assert.assertEquals(epochStringParam, event.parameter(epochStringKey));
        Exception ex = Assert.assertThrows(NoSuchElementException.class, () -> event.parameter(notUsedKey));
        Assert.assertEquals(ex.getMessage(), "Parameter set does not contain key: "+notUsedKey.keyName());

        // jMissingKeys
        var expectedMissingKeys = Set.of(notUsedKey.keyName());
        Set<String> jMissingKeys = event.jMissingKeys(encoderIntKey, epochStringKey, notUsedKey);
        Assert.assertEquals(expectedMissingKeys, jMissingKeys);

        // getStringMap
        List<String> encoderStringParam = encoderParam.jValues().stream().map(Object::toString)
                .collect(Collectors.toList());
        Map<String, String> expectedParamMap = Map.of(
                encoderParam.keyName(), String.join(",", encoderStringParam),
                epochStringParam.keyName(), String.join(",", epochStringParam.jValues())
        );
        Assert.assertEquals(expectedParamMap, event.jGetStringMap());
    }

    @Test
    public void shouldAbleToCreateAndAccessSystemEvent__DEOPSCSW_327_DEOPSCSW_328_DEOPSCSW_185_DEOPSCSW_329_DEOPSCSW_183() {
        SystemEvent systemEvent = new SystemEvent(prefix, new EventName("filter wheel")).add(encoderParam).add(epochStringParam);
        assertOnEventsAPI(systemEvent,new EventName("filter wheel"));
    }

    @Test
    public void shouldAbleToRemoveParamsInSystemEvent__DEOPSCSW_327_DEOPSCSW_328_DEOPSCSW_185_DEOPSCSW_329_DEOPSCSW_183() {
        SystemEvent systemEvent = new SystemEvent(prefix, new EventName("filter wheel")).add(encoderParam).add(epochByteParam);
        Assert.assertEquals(2, systemEvent.size());
        Assert.assertArrayEquals(new Byte[]{10, 20}, systemEvent.jGet(epochByteKey).orElseThrow().jValues().toArray());
        SystemEvent mutatedEvent = systemEvent.remove(encoderParam);
        Assert.assertEquals(1, mutatedEvent.size());
    }

    @Test
    public void shouldAbleToRemoveParamsInObserveEvent__DEOPSCSW_327_DEOPSCSW_328_DEOPSCSW_185_DEOPSCSW_329_DEOPSCSW_183() {
        ObserveEvent observeEvent = WFSDetectorEvent.publishSuccess(prefix).add(encoderParam).add(epochByteParam);
        Assert.assertEquals(2, observeEvent.size());
        Assert.assertArrayEquals(new Byte[]{10, 20}, observeEvent.jGet(epochByteKey).orElseThrow().jValues().toArray());
        ObserveEvent mutatedEvent = observeEvent.remove(encoderParam);
        Assert.assertEquals(1, mutatedEvent.size());
    }

    @Test
    public void shouldBeUniqueWhenParametersAreAddedOrRemovedForSystem__DEOPSCSW_327_DEOPSCSW_328_DEOPSCSW_185_DEOPSCSW_329_DEOPSCSW_183() {
        SystemEvent systemEvent1 = new SystemEvent(prefix, new EventName("filter wheel")).add(encoderParam).add(epochStringParam);
        SystemEvent systemEvent2 = systemEvent1.add(epochIntParam);
        SystemEvent systemEvent3 = systemEvent2.remove(epochIntKey);

        assertOnEventsAPI(systemEvent1, new EventName("filter wheel"));
        Assert.assertNotEquals(systemEvent1.eventId(), systemEvent2.eventId());
        Assert.assertEquals(systemEvent1.eventName(), systemEvent2.eventName());
        Assert.assertEquals(systemEvent1.source(), systemEvent2.source());

        Assert.assertNotEquals(systemEvent3.eventId(), systemEvent2.eventId());
        Assert.assertEquals(systemEvent3.eventName(), systemEvent2.eventName());
        Assert.assertEquals(systemEvent3.source(), systemEvent2.source());
    }

    @Test
    public void shouldBeUniqueIdWhenParametersAreAddedOrRemovedForObserve__DEOPSCSW_327_DEOPSCSW_328_DEOPSCSW_185_DEOPSCSW_329_DEOPSCSW_183() {
        ObserveEvent observeEvent1 = WFSDetectorEvent.publishSuccess(prefix).add(encoderParam).add(epochStringParam);
        ObserveEvent observeEvent2 = observeEvent1.add(epochIntParam);
        ObserveEvent observeEvent3 = observeEvent2.remove(epochIntKey);
        System.out.println(observeEvent1.paramSet());
        assertOnEventsAPI(observeEvent1,new EventName("ObserveEvent.PublishSuccess"));
        Assert.assertNotEquals(observeEvent1.eventId(), observeEvent2.eventId());
        Assert.assertEquals(observeEvent1.eventName(), observeEvent2.eventName());
        Assert.assertEquals(observeEvent1.source(), observeEvent2.source());

        Assert.assertNotEquals(observeEvent3.eventId(), observeEvent2.eventId());
        Assert.assertEquals(observeEvent3.eventName(), observeEvent2.eventName());
        Assert.assertEquals(observeEvent3.source(), observeEvent2.source());

    }
}
