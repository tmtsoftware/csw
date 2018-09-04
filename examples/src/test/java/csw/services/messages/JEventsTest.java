package csw.services.messages;

import csw.messages.events.EventName;
import csw.messages.events.EventTime;
import csw.messages.events.ObserveEvent;
import csw.messages.events.SystemEvent;
import csw.messages.javadsl.JUnits;
import csw.messages.params.formats.JavaJsonSupport;
import csw.messages.params.generics.JKeyType;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.MatrixData;
import csw.messages.params.models.Prefix;
import csw.messages.params.models.RaDec;
import csw_protobuf.events.PbEvent;
import org.junit.Assert;
import org.junit.Test;
import play.api.libs.json.JsValue;
import play.api.libs.json.Json;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class JEventsTest {

    @Test
    public void showUsageOfEventTime() {
        //#eventtime

        //apply returns current time in UTC
        EventTime now = EventTime.apply();

        //using constructor
        EventTime anHourAgo = new EventTime(Instant.now().minusSeconds(3600));

        //return current time in UTC
        EventTime currentTime = EventTime.apply();

        //some past time using utility function
        EventTime aDayAgo = EventTime.apply(Instant.now().minusSeconds(86400));

        //#eventtime

        //validations
        Assert.assertTrue(now.time().isAfter(anHourAgo.time()));
        Assert.assertTrue(anHourAgo.time().isAfter(aDayAgo.time()));
        Assert.assertTrue(currentTime.time().isAfter(anHourAgo.time()));
    }

    @Test
    public void showUsageOfSystemEvent() {
        //#systemevent
        //keys
        Key<Integer> k1 = JKeyType.IntKey().make("encoder");
        Key<Integer> k2 = JKeyType.IntKey().make("speed");
        Key<String> k3 = JKeyType.StringKey().make("filter");
        Key<Integer> k4 = JKeyType.IntKey().make("notUsed");

        //prefixes
        Prefix prefix1 = new Prefix("wfos.red.filter");
        EventName name1 = new EventName("filterWheel");
        Prefix prefix2 = new Prefix("iris.imager.filter");
        EventName name2 = new EventName("status");

        //parameters
        Parameter<Integer> p1 = k1.set(22);
        Parameter<Integer> p2 = k2.set(44);
        Parameter<String> p3 = k3.set("A", "B", "C", "D");

        //Create SystemEvent using madd
        SystemEvent se1 = new SystemEvent(prefix1, name1).madd(p1, p2);
        //Create SystemEvent using add
        SystemEvent se2 = new SystemEvent(prefix2, name2).add(p1).add(p2);
        //Create SystemEvent and use add
        SystemEvent se3 = new SystemEvent(prefix2, name2).add(p1).add(p2).add(p3);

        //access keys
        Boolean k1Exists = se1.exists(k1); //true

        //access Parameters
        Optional<Parameter<Integer>> p4 = se1.jGet(k1);

        //access values
        List<Integer> v1 = se1.jGet(k1).get().jValues();
        List<Integer> v2 = se2.parameter(k2).jValues();
        //k4 is missing
        Set<String> missingKeys = se3.jMissingKeys(k1, k2, k3, k4);

        //remove keys
        SystemEvent se4 = se3.remove(k3);
        //#systemevent

        Assert.assertTrue(k1Exists);
        Assert.assertTrue(p4.get() == p1);
        Assert.assertEquals(new HashSet<>(Arrays.asList(22)), new HashSet<>(v1));
        Assert.assertEquals(new HashSet<>(Arrays.asList(44)), new HashSet<>(v2));
        Assert.assertEquals(new HashSet<>(Arrays.asList(missingKeys)), new HashSet<>(Arrays.asList(missingKeys)));
        Assert.assertNotEquals(se3.eventId(), se4.eventId()); //Test unique id when parameters are removed
    }

    @Test
    public void showUsageOfObserveEvent() {
        //#observeevent
        //keys
        Key<Integer> k1 = JKeyType.IntKey().make("readoutsCompleted");
        Key<Integer> k2 = JKeyType.IntKey().make("coaddsCompleted");
        Key<String> k3 = JKeyType.StringKey().make("fileID");
        Key<Integer> k4 = JKeyType.IntKey().make("notUsed");

        //prefixes
        Prefix prefix1 = new Prefix("iris.ifu.detectorAssembly");
        EventName name1 = new EventName("readoutEnd");
        Prefix prefix2 = new Prefix("wfos.red.detector");
        EventName name2 = new EventName("exposureStarted");

        //parameters
        Parameter<Integer> p1 = k1.set(4);
        Parameter<Integer> p2 = k2.set(2);
        Parameter<String> p3 = k3.set("WFOS-RED-0001");

        //Create ObserveEvent using madd
        ObserveEvent oc1 = new ObserveEvent(prefix1, name1).madd(p1, p2);
        //Create ObserveEvent using add
        ObserveEvent oc2 = new ObserveEvent(prefix2, name2).add(p1).add(p2);
        //Create ObserveEvent and use add
        ObserveEvent oc3 = new ObserveEvent(prefix2, name2).add(p1).add(p2).add(p3);

        //access keys
        Boolean k1Exists = oc1.exists(k1); //true

        //access Parameters
        Optional<Parameter<Integer>> p4 = oc1.jGet(k1);

        //access values
        List<Integer> v1 = oc1.jGet(k1).get().jValues();
        List<Integer> v2 = oc2.parameter(k2).jValues();
        //k4 is missing
        Set<String> missingKeys = oc3.jMissingKeys(k1, k2, k3, k4);

        //remove keys
        ObserveEvent oc4 = oc3.remove(k3);
        //#observeevent

        Assert.assertTrue(k1Exists);
        Assert.assertTrue(p4.get() == p1);
        Assert.assertEquals(new HashSet<>(Arrays.asList(4)), new HashSet<>(v1));
        Assert.assertEquals(new HashSet<>(Arrays.asList(2)), new HashSet<>(v2));
        Assert.assertEquals(new HashSet<>(Arrays.asList(missingKeys)), new HashSet<>(Arrays.asList(missingKeys)));
        Assert.assertNotEquals(oc3.eventId(), oc4.eventId()); //Test unique id when parameters are removed
    }

    @Test
    public void showUsageOfJsonSerialization() {
        //#json-serialization
        //key
        Key<MatrixData<Double>> k1 = JKeyType.DoubleMatrixKey().make("myMatrix");

        //prefixes
        Prefix prefix1 = new Prefix("aoesw.rpg");
        EventName name1 = new EventName("correctionInfo");

        //values
        Double[][] doubles = {{1.0, 2.0, 3.0}, {4.1, 5.1, 6.1}, {7.2, 8.2, 9.2}};
        MatrixData<Double> m1 = MatrixData.fromJavaArrays(Double.class, doubles);

        //parameter
        Parameter<MatrixData<Double>> i1 = k1.set(m1);


        //events
        ObserveEvent observeEvent = new ObserveEvent(prefix1, name1).add(i1);
        SystemEvent systemEvent = new SystemEvent(prefix1, name1).add(i1);

        //json support - write
        JsValue observeJson = JavaJsonSupport.writeEvent(observeEvent);
        JsValue systemJson = JavaJsonSupport.writeEvent(systemEvent);

        //optionally prettify
        String str = Json.prettyPrint(systemJson);

        //construct DemandState from string
        SystemEvent statusFromPrettyStr = JavaJsonSupport.readEvent(Json.parse(str));

        //json support - read
        ObserveEvent observeEvent1 = JavaJsonSupport.readEvent(observeJson);
        SystemEvent systemEvent1 = JavaJsonSupport.readEvent(systemJson);
        //#json-serialization

        //validations
        Assert.assertTrue(systemEvent.equals(statusFromPrettyStr));
        Assert.assertTrue(observeEvent.equals(observeEvent1));
        Assert.assertTrue(systemEvent.equals(systemEvent1));
    }

    @Test
    public void showUniqueKeyConstraintExample() {
        //#unique-key
        //keys
        Key<Integer> encoderKey = JKeyType.IntKey().make("encoder");
        Key<Integer> filterKey = JKeyType.IntKey().make("filter");
        Key<Integer> miscKey = JKeyType.IntKey().make("misc");

        //prefix
        Prefix prefix1 = new Prefix("wfos.blue.filter");
        EventName name1 = new EventName("filterWheel");

        //params
        Parameter<Integer> encParam1 = encoderKey.set(1);
        Parameter<Integer> encParam2 = encoderKey.set(2);
        Parameter<Integer> encParam3 = encoderKey.set(3);

        Parameter<Integer> filterParam1 = filterKey.set(1);
        Parameter<Integer> filterParam2 = filterKey.set(2);
        Parameter<Integer> filterParam3 = filterKey.set(3);

        Parameter<Integer> miscParam1 = miscKey.set(100);

        //StatusEvent with duplicate key via madd
        SystemEvent event = new SystemEvent(prefix1, name1).madd(
                encParam1,
                encParam2,
                encParam3,
                filterParam1,
                filterParam2,
                filterParam3);
        //four duplicate keys are removed; now contains one Encoder and one Filter key
        List<String> uniqueKeys1 = event.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());

        //try adding duplicate keys via add + madd
        SystemEvent changedEvent = event.add(encParam3).madd(filterParam1, filterParam2, filterParam3);
        //duplicate keys will not be added. Should contain one Encoder and one Filter key
        List<String> uniqueKeys2 = changedEvent.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());

        //miscKey(unique) will be added; encoderKey(duplicate) will not be added
        SystemEvent finalEvent = changedEvent.madd(miscParam1, encParam1);
        //now contains encoderKey, filterKey, miscKey
        List<String> uniqueKeys3 = finalEvent.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());
        //#unique-key

        //validations
        Assert.assertEquals(new HashSet<>(uniqueKeys1), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName())));
        Assert.assertEquals(new HashSet<>(uniqueKeys2), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName())));
        Assert.assertEquals(new HashSet<>(uniqueKeys3), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName(), miscKey.keyName())));
    }

    @Test
    public void showUsageOfProtobuf() {
        //#protobuf

        //prefixes
        Prefix prefix1 = new Prefix("tcs.pk");
        EventName name1 = new EventName("targetCoords");
        Prefix prefix2 = new Prefix("tcs.cm");
        EventName name2 = new EventName("guiderCoords");

        //Key
        Key<RaDec> raDecKey = JKeyType.RaDecKey().make("raDecKey");

        //values
        RaDec raDec1 = new RaDec(10.20, 40.20);
        RaDec raDec2 = new RaDec(11.20, 50.20);

        //parameters
        Parameter<RaDec> param = raDecKey.set(raDec1, raDec2).withUnits(JUnits.arcmin);

        //events
        ObserveEvent observeEvent = new ObserveEvent(prefix1, name1).add(param);
        SystemEvent systemEvent1 = new SystemEvent(prefix1, name1).add(param);
        SystemEvent systemEvent2 = new SystemEvent(prefix2, name2).add(param);

        //convert events to protobuf bytestring
        PbEvent byteArray2 = observeEvent.toPb();
        PbEvent byteArray3 = systemEvent1.toPb();
        PbEvent byteArray4 = systemEvent2.toPb();

        //convert protobuf bytestring to events
        ObserveEvent pbObserveEvent = ObserveEvent.fromPb(byteArray2);
        SystemEvent pbSystemEvent1 = SystemEvent.fromPb(byteArray3);
        SystemEvent pbSystemEvent2 = SystemEvent.fromPb(byteArray4);
        //#protobuf

        //validations
        Assert.assertEquals(pbObserveEvent, observeEvent);
        Assert.assertEquals(pbSystemEvent1, systemEvent1);
        Assert.assertEquals(pbSystemEvent2, systemEvent2);
    }
}
