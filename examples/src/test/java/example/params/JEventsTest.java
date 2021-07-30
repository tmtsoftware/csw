package example.params;

import csw.params.core.formats.EventCbor$;
import csw.params.core.formats.JavaJsonSupport;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Coords;
import csw.params.core.models.JCoords;
import csw.params.core.models.MatrixData;
import csw.params.core.models.ObsId;
import csw.params.events.EventName;
import csw.params.events.IRDetectorEvent;
import csw.params.events.ObserveEvent;
import csw.params.events.SystemEvent;
import csw.params.javadsl.JKeyType;
import csw.prefix.models.Prefix;
import csw.prefix.javadsl.JSubsystem;
import csw.params.javadsl.JUnits;
import csw.time.core.models.UTCTime;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import play.api.libs.json.JsValue;
import play.api.libs.json.Json;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

//DEOPSCSW-331: Event Service Accessible to all CSW component builders
public class JEventsTest extends JUnitSuite {

    @Test
    public void showUsageOfEventTime__DEOPSCSW_331() {
        //#eventtime

        //apply returns current time in UTC
        UTCTime now = UTCTime.now();

        //using constructor
        UTCTime anHourAgo = new UTCTime(Instant.now().minusSeconds(3600));

        //return current time in UTC
        UTCTime currentTime = UTCTime.now();

        //some past time using utility function
        UTCTime aDayAgo = new UTCTime(Instant.now().minusSeconds(86400));

        //#eventtime

        //validations
        Assert.assertTrue(now.value().isAfter(anHourAgo.value()));
        Assert.assertTrue(anHourAgo.value().isAfter(aDayAgo.value()));
        Assert.assertTrue(currentTime.value().isAfter(anHourAgo.value()));
    }

    @Test
    public void showUsageOfSystemEvent__DEOPSCSW_331() {
        //#systemevent
        //keys
        Key<Integer> k1 = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Key<Integer> k2 = JKeyType.IntKey().make("speed");
        Key<String> k3 = JKeyType.StringKey().make("filter");
        Key<Integer> k4 = JKeyType.IntKey().make("notUsed");

        //prefixes
        Prefix prefix1 = Prefix.apply(JSubsystem.WFOS, "red.filter");
        EventName name1 = new EventName("filterWheel");
        Prefix prefix2 = Prefix.apply(JSubsystem.IRIS, "imager.filter");
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
        boolean k1Exists = se1.exists(k1); //true

        //access Parameters
        Optional<Parameter<Integer>> p4 = se1.jGet(k1);

        //access values
        List<Integer> v1 = se1.jGet(k1).orElseThrow().jValues();
        List<Integer> v2 = se2.parameter(k2).jValues();
        //k4 is missing
        Set<String> missingKeys = se3.jMissingKeys(k1, k2, k3, k4);

        //remove keys
        SystemEvent se4 = se3.remove(k3);
        //#systemevent

        Assert.assertTrue(k1Exists);
        Assert.assertSame(p4.orElseThrow(), p1);
        Assert.assertEquals(Set.of(22), Set.copyOf(v1));
        Assert.assertEquals(Set.of(44), Set.copyOf(v2));
        Assert.assertNotEquals(se3.eventId(), se4.eventId()); //Test unique id when parameters are removed
    }

    @Test
    public void showUsageOfJsonSerialization__DEOPSCSW_331() {
        //#json-serialization
        //key
        Key<MatrixData<Double>> k1 = JKeyType.DoubleMatrixKey().make("myMatrix");

        //prefixes
        Prefix prefix1 = Prefix.apply(JSubsystem.AOESW, "rpg");
        EventName name1 = new EventName("correctionInfo");

        //values
        Double[][] doubles = {{1.0, 2.0, 3.0}, {4.1, 5.1, 6.1}, {7.2, 8.2, 9.2}};
        MatrixData<Double> m1 = MatrixData.fromArrays(doubles);

        //parameter
        Parameter<MatrixData<Double>> i1 = k1.set(m1);


        //events
        ObserveEvent observeEvent = IRDetectorEvent.observeStart(prefix1, ObsId.apply("1234A-123-124"));
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
        Assert.assertEquals(systemEvent, statusFromPrettyStr);
        Assert.assertEquals(observeEvent, observeEvent1);
        Assert.assertEquals(systemEvent, systemEvent1);
    }

    @Test
    public void showUniqueKeyConstraintExample__DEOPSCSW_331() {
        //#unique-key
        //keys
        Key<Integer> encoderKey = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Key<Integer> filterKey = JKeyType.IntKey().make("filter");
        Key<Integer> miscKey = JKeyType.IntKey().make("misc");

        //prefix
        Prefix prefix1 = Prefix.apply(JSubsystem.WFOS, "blue.filter");
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
        Set<String> uniqueKeys1 = event.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toUnmodifiableSet());

        //try adding duplicate keys via add + madd
        SystemEvent changedEvent = event.add(encParam3).madd(filterParam1, filterParam2, filterParam3);
        //duplicate keys will not be added. Should contain one Encoder and one Filter key
        Set<String> uniqueKeys2 = changedEvent.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toUnmodifiableSet());

        //miscKey(unique) will be added; encoderKey(duplicate) will not be added
        SystemEvent finalEvent = changedEvent.madd(miscParam1, encParam1);
        //now contains encoderKey, filterKey, miscKey
        Set<String> uniqueKeys3 = finalEvent.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toUnmodifiableSet());
        //#unique-key

        //validations
        Assert.assertEquals(uniqueKeys1, Set.of(encoderKey.keyName(), filterKey.keyName()));
        Assert.assertEquals(uniqueKeys2, Set.of(encoderKey.keyName(), filterKey.keyName()));
        Assert.assertEquals(uniqueKeys3, Set.of(encoderKey.keyName(), filterKey.keyName(), miscKey.keyName()));
    }

    @Test
    public void showUsageOfCbor__DEOPSCSW_331__CSW_147() {
        //#cbor
        //#observe-event
        //prefixes
        Prefix prefix1 = Prefix.apply(JSubsystem.TCS, "pk");
        EventName name1 = new EventName("targetCoords");
        Prefix prefix2 = Prefix.apply(JSubsystem.TCS, "cm");
        EventName name2 = new EventName("guiderCoords");

        //Key
        Key<Coords.SolarSystemCoord> planets = JKeyType.SolarSystemCoordKey().make("planets", JUnits.NoUnits);

        //values
        Coords.SolarSystemCoord planet1 = new Coords.SolarSystemCoord(new Coords.Tag("solar1"), JCoords.Jupiter());
        Coords.SolarSystemCoord planet2 =  new Coords.SolarSystemCoord(new Coords.Tag("solar2"), JCoords.Venus());

        //parameters
        Parameter<Coords.SolarSystemCoord> param = planets.set(planet1, planet2);
        //events

        ObserveEvent observeEvent = IRDetectorEvent.observeStart(prefix1, ObsId.apply("1234A-123-124"));
        //#observe-event
        SystemEvent systemEvent1 = new SystemEvent(prefix1, name1).add(param);
        SystemEvent systemEvent2 = new SystemEvent(prefix2, name2).add(param);
        //convert events to cbor bytestring
        byte[] byteArray2 = EventCbor$.MODULE$.encode(observeEvent);
        byte[] byteArray3 = EventCbor$.MODULE$.encode(systemEvent1);
        byte[] byteArray4 = EventCbor$.MODULE$.encode(systemEvent2);

        //convert cbor bytestring to events
        ObserveEvent pbObserveEvent = EventCbor$.MODULE$.decode(byteArray2);
        SystemEvent pbSystemEvent1 = EventCbor$.MODULE$.decode(byteArray3);
        SystemEvent pbSystemEvent2 = EventCbor$.MODULE$.decode(byteArray4);
        //#cbor

        //validations
        Assert.assertEquals(pbObserveEvent, observeEvent);
        Assert.assertEquals(pbSystemEvent1, systemEvent1);
        Assert.assertEquals(pbSystemEvent2, systemEvent2);
    }
}
