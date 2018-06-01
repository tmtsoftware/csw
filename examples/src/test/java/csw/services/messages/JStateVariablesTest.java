package csw.services.messages;

import csw.messages.params.formats.JavaJsonSupport;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.MatrixData;
import csw.messages.params.models.ObsId;
import csw.messages.params.states.CurrentState;
import csw.messages.params.states.DemandState;
import csw.messages.params.states.StateName;
import org.junit.Assert;
import org.junit.Test;
import play.api.libs.json.JsValue;
import play.api.libs.json.Json;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static csw.messages.javadsl.JUnits.NoUnits;
import static csw.messages.javadsl.JUnits.meter;

public class JStateVariablesTest {

    @Test
    public void showUsageOfDemandState() {
        //#demandstate
        //prefix
        String prefix = "wfos.prog.cloudcover";

        //keys
        Key<Character> charKey = JKeyTypes.CharKey().make("charKey");
        Key<Integer> intKey = JKeyTypes.IntKey().make("intKey");
        Key<Boolean> booleanKey = JKeyTypes.BooleanKey().make("booleanKey");
        Key<Instant> timestampKey = JKeyTypes.TimestampKey().make("timestampKey");
        Key<String> notUsedKey = JKeyTypes.StringKey().make("notUsed");


        //#obsid
        ObsId obsId = new ObsId("Obs001");

        //parameters
        Parameter<Character> charParam = charKey.set('A', 'B', 'C').withUnits(NoUnits);
        Parameter<Integer> intParam = intKey.set(1, 2, 3).withUnits(meter);
        Parameter<Boolean> booleanParam = booleanKey.set(true, false);
        Parameter<Instant> timestamp = timestampKey.set(Instant.now());

        //create DemandState and use sequential add
        DemandState ds1 = new DemandState(prefix, new StateName("testStateName")).add(charParam).add(intParam);
        //create DemandState and add more than one Parameters using madd
        DemandState ds2 = new DemandState(prefix, new StateName("testStateName")).madd(intParam, booleanParam);
        //create DemandState using apply
        DemandState ds3 = new DemandState(prefix, new StateName("testStateName")).add(timestamp);

        //access keys
        Boolean charKeyExists = ds1.exists(charKey); //true

        //access Parameters
        Optional<Parameter<Integer>> p1 = ds1.jGet(intKey);

        //access values
        List<Character> v1 = ds1.jGet(charKey).get().jValues();
        List<Boolean> v2 = ds2.parameter(booleanKey).jValues();
        Set<String> missingKeys = ds3.jMissingKeys(charKey,
                intKey,
                booleanKey,
                timestampKey,
                notUsedKey);

        //remove keys
        DemandState ds4 = ds3.remove(timestampKey);

        //update existing keys - set it back by an hour
        DemandState ds5 = ds3.add(timestampKey.set(Instant.now().minusSeconds(3600)));
        //#demandstate

        //validations
        Assert.assertTrue(charKeyExists);
        Assert.assertTrue(p1.get() == intParam);
        Assert.assertEquals(new HashSet<>(Arrays.asList('A', 'B', 'C')), new HashSet<>(v1));
        Assert.assertEquals(new HashSet<>(Arrays.asList(true, false)), new HashSet<>(v2));
        Assert.assertTrue(4 == missingKeys.size());
        Assert.assertTrue(false == ds4.exists(timestampKey));
        Assert.assertTrue(ds5.jGet(timestampKey).get().head().isBefore(ds3.jGet(timestampKey).get().head()));
    }

    @Test
    public void showUsageOfCurrentState() {
        //#currentstate
        //prefix
        String prefix = "wfos.prog.cloudcover";

        //keys
        Key<Character> charKey = JKeyTypes.CharKey().make("charKey");
        Key<Integer> intKey = JKeyTypes.IntKey().make("intKey");
        Key<Boolean> booleanKey = JKeyTypes.BooleanKey().make("booleanKey");
        Key<Instant> timestampKey = JKeyTypes.TimestampKey().make("timestampKey");
        Key<String> notUsedKey = JKeyTypes.StringKey().make("notUsed");


        //#obsid
        ObsId obsId = new ObsId("Obs001");

        //parameters
        Parameter<Character> charParam = charKey.set('A', 'B', 'C').withUnits(NoUnits);
        Parameter<Integer> intParam = intKey.set(1, 2, 3).withUnits(meter);
        Parameter<Boolean> booleanParam = booleanKey.set(true, false);
        Parameter<Instant> timestamp = timestampKey.set(Instant.now());

        //create CurrentState and use sequential add
        CurrentState cs1 = new CurrentState(prefix, new StateName("testStateName")).add(charParam).add(intParam);
        //create CurrentState and add more than one Parameters using madd
        CurrentState cs2 = new CurrentState(prefix, new StateName("testStateName")).madd(intParam, booleanParam);
        //create CurrentState using apply
        CurrentState cs3 = new CurrentState(prefix, new StateName("testStateName")).add(timestamp);

        //access keys
        Boolean charKeyExists = cs1.exists(charKey); //true

        //access Parameters
        Optional<Parameter<Integer>> p1 = cs1.jGet(intKey);

        //access values
        List<Character> v1 = cs1.jGet(charKey).get().jValues();
        List<Boolean> v2 = cs2.parameter(booleanKey).jValues();
        Set<String> missingKeys = cs3.jMissingKeys(charKey,
                intKey,
                booleanKey,
                timestampKey,
                notUsedKey);

        //remove keys
        CurrentState cs4 = cs3.remove(timestampKey);

        //update existing keys - set it back by an hour
        CurrentState cs5 = cs3.add(timestampKey.set(Instant.now().minusSeconds(3600)));
        //#currentstate

        //validations
        Assert.assertTrue(charKeyExists);
        Assert.assertTrue(p1.get() == intParam);
        Assert.assertEquals(new HashSet<>(Arrays.asList('A', 'B', 'C')), new HashSet<>(v1));
        Assert.assertEquals(new HashSet<>(Arrays.asList(true, false)), new HashSet<>(v2));
        Assert.assertTrue(4 == missingKeys.size());
        Assert.assertTrue(false == cs4.exists(timestampKey));
        Assert.assertTrue(cs5.jGet(timestampKey).get().head().isBefore(cs3.jGet(timestampKey).get().head()));
    }

    @Test
    public void showJsonSerialization() {
        //#json-serialization
        //key
        Key<MatrixData<Double>> k1 = JKeyTypes.DoubleMatrixKey().make("myMatrix");

        //values
        Double[][] doubles = {{1.0, 2.0, 3.0}, {4.1, 5.1, 6.1}, {7.2, 8.2, 9.2}};
        MatrixData<Double> m1 = MatrixData.fromJavaArrays(Double.class, doubles);

        //parameter
        Parameter<MatrixData<Double>> i1 = k1.set(m1);

        //state variables
        DemandState ds = new DemandState("wfos.blue.filter", new StateName("testStateName")).add(i1);
        CurrentState cs = new CurrentState("wfos.blue.filter", new StateName("testStateName")).add(i1);

        //json support - write
        JsValue dsJson = JavaJsonSupport.writeStateVariable(ds);
        JsValue csJson = JavaJsonSupport.writeStateVariable(cs);

        //optionally prettify
        String str = Json.prettyPrint(dsJson);

        //construct DemandState from string
        DemandState dsFromPrettyStr = JavaJsonSupport.readStateVariable(Json.parse(str));

        //json support - read
        DemandState ds1 = JavaJsonSupport.readStateVariable(dsJson);
        CurrentState cs1 = JavaJsonSupport.readStateVariable(csJson);
        //#json-serialization

        //validations
        Assert.assertTrue(ds.equals(ds1));
        Assert.assertTrue(cs.equals(cs1));
        Assert.assertTrue(dsFromPrettyStr.equals(ds1));
    }

    @Test
    public void showUniqueKeyConstraintExample() {
        //#unique-key
        //keys
        Key<Integer> encoderKey = JKeyTypes.IntKey().make("encoder");
        Key<Integer> filterKey = JKeyTypes.IntKey().make("filter");
        Key<Integer> miscKey = JKeyTypes.IntKey().make("misc.");

        //prefix
        String prefix = "wfos.blue.filter";

        //params
        Parameter<Integer> encParam1 = encoderKey.set(1);
        Parameter<Integer> encParam2 = encoderKey.set(2);
        Parameter<Integer> encParam3 = encoderKey.set(3);

        Parameter<Integer> filterParam1 = filterKey.set(1);
        Parameter<Integer> filterParam2 = filterKey.set(2);
        Parameter<Integer> filterParam3 = filterKey.set(3);

        Parameter<Integer> miscParam1 = miscKey.set(100);

        //Demand state with duplicate key via madd
        DemandState state = new DemandState(prefix, new StateName("testStateName")).madd(
                encParam1,
                encParam2,
                encParam3,
                filterParam1,
                filterParam2,
                filterParam3);
        //four duplicate keys are removed; now contains one Encoder and one Filter key
        List<String> uniqueKeys1 = state.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());

        //try adding duplicate keys via add + madd
        DemandState changedState = state.add(encParam3).madd(filterParam1, filterParam2, filterParam3);
        //duplicate keys will not be added. Should contain one Encoder and one Filter key
        List<String> uniqueKeys2 = changedState.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());

        //miscKey(unique) will be added; encoderKey(duplicate) will not be added
        DemandState finalState = changedState.madd(miscParam1, encParam1);
        //now contains encoderKey, filterKey, miscKey
        List<String> uniqueKeys3 = finalState.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());
        //#unique-key

        //validations
        Assert.assertEquals(new HashSet<>(uniqueKeys1), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName())));
        Assert.assertEquals(new HashSet<>(uniqueKeys2), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName())));
        Assert.assertEquals(new HashSet<>(uniqueKeys3), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName(), miscKey.keyName())));
    }
}
