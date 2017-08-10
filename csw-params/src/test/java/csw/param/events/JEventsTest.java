package csw.param.events;

import csw.param.models.ObsId;
import csw.param.models.Prefix;
import csw.param.generics.JKeyTypes;
import csw.param.generics.Key;
import csw.param.generics.Parameter;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static csw.param.javadsl.JSubsystem.WFOS;

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

    private final String prefix = "wfos.red.detector";

    private void assertOnEventsAPI(EventType<?> event) {
        // eventInfo, prefix, subsystem, source
        Assert.assertEquals(EventInfo.create(prefix), event.info());
        Assert.assertEquals(prefix, event.prefixStr());
        Assert.assertEquals(new Prefix(prefix), event.prefix());
        Assert.assertEquals(prefix, event.source());
        Assert.assertEquals(WFOS, event.subsystem());

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
    public void shouldAbleToCreateAndAccessStatusEvent() {
        StatusEvent statusEvent = new StatusEvent(prefix).add(encoderParam).add(epochStringParam);
        assertOnEventsAPI(statusEvent);
    }

    @Test
    public void shouldAbleToCreateAndAccessObserveEvent() {
        ObserveEvent observeEvent = new ObserveEvent(prefix).add(encoderParam).add(epochStringParam);
        assertOnEventsAPI(observeEvent);
    }

    @Test
    public void shouldAbleToCreateAndAccessSystemEvent() {
        SystemEvent systemEvent = new SystemEvent(prefix).add(encoderParam).add(epochStringParam);
        assertOnEventsAPI(systemEvent);
    }

    @Test
    public void shouldAbleToCreateAndAccessStatusEventWithCustomInfo() {
        Instant currentTime = Instant.now();
        EventTime eventTime = new EventTime(currentTime);
        ObsId obsId = ObsId.apply("obsId");
        EventInfo eventInfo = EventInfo.apply(prefix, eventTime, obsId);

        StatusEvent statusEvent = new StatusEvent(prefix, eventTime, obsId).add(encoderParam).add(epochStringParam);
        Assert.assertEquals(eventInfo, statusEvent.info());
        Assert.assertEquals(eventTime, statusEvent.eventTime());
        Assert.assertEquals(Optional.of(obsId), statusEvent.obsIdOptional());
    }

}
