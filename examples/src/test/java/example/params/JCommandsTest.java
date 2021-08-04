package example.params;

import csw.params.commands.CommandName;
import csw.params.commands.Observe;
import csw.params.commands.Setup;
import csw.params.commands.Wait;
import csw.params.core.formats.JavaJsonSupport;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.ArrayData;
import csw.params.core.models.MatrixData;
import csw.params.core.models.ObsId;
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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JCommandsTest extends JUnitSuite {
    //#obsid
    final ObsId obsId = ObsId.apply("2020A-001-123");
    //#obsid

    @Test
    public void showUsageOfUtilityFunctions() {
        //#prefix
        //using constructor, supplying subsystem and prefix both
        Prefix prefix1 = Prefix.apply(JSubsystem.NFIRAOS, "ncc.trombone");

        //just by supplying prefix
        Prefix prefix2 = Prefix.apply(JSubsystem.TCS, "mobie.blue.filter");

        //invalid prefix string which does not contain valid subsystem in the beginning will throw an exception,
        // Prefix badPrefix = Prefix.apply("abcdefgh");
        //#prefix

        //validations
        Assert.assertSame(prefix1.subsystem(), JSubsystem.NFIRAOS);
        Assert.assertSame(prefix2.subsystem(), JSubsystem.TCS);
    }

    @Test
    public void showUsageOfSetupCommand() {
        //#setup
        //keys
        Key<Integer> k1 = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Key<String> k2 = JKeyType.StringKey().make("stringThing");
        Key<Integer> k2bad = JKeyType.IntKey().make("missingKey");
        Key<Integer> k3 = JKeyType.IntKey().make("filter");
        Key<Float> k4 = JKeyType.FloatKey().make("correction");

        //prefix
        Prefix prefix = Prefix.apply(JSubsystem.WFOS, "red.detector");

        //parameters
        Parameter<Integer> i1 = k1.set(22);
        Parameter<String> i2 = k2.set("A");

        //create setup, add sequentially using add
        Setup sc1 = new Setup(prefix, new CommandName("move"), Optional.of(obsId)).add(i1).add(i2);

        //access keys
        boolean k1Exists = sc1.exists(k1); //true

        //access parameters
        Optional<Parameter<Integer>> optParam1 = sc1.jGet(k1); //present
        Optional<Parameter<Integer>> optK2Bad = sc1.jGet(k2bad); //absent

        //add more than one parameters, using madd
        Setup sc2 = sc1.madd(k3.set(1, 2, 3, 4).withUnits(JUnits.day), k4.set(1.0f, 2.0f));
        int paramSize = sc2.size();

        //add binary payload
        Key<Byte> byteKey1 = JKeyType.ByteKey().make("byteKey1");
        Key<Byte> byteKey2 = JKeyType.ByteKey().make("byteKey2");
        Byte[] bytes1 = {10, 20};
        Byte[] bytes2 = {30, 40};

        Parameter<Byte> b1 = byteKey1.setAll(bytes1);
        Parameter<Byte> b2 = byteKey2.setAll(bytes2);

        Setup sc3 = new Setup(prefix, new CommandName("move"), Optional.of(obsId)).add(b1).add(b2);

        //remove a key
        Setup sc4 = sc3.remove(b1);

        //list all keys
        java.util.List<String> allKeys = sc4.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());
        //#setup

        //validations
        Assert.assertTrue(k1Exists);
        Assert.assertTrue(optParam1.isPresent());
        Assert.assertFalse(optK2Bad.isPresent());
        Assert.assertEquals(4, paramSize);
        Assert.assertEquals(2, sc3.size());
        Assert.assertEquals(1, sc4.size());
        Assert.assertEquals(1, allKeys.size());
    }

    @Test
    public void showUsageOfObserveCommand() {
        //#observe
        //keys
        Key<Boolean> k1 = JKeyType.BooleanKey().make("repeat");
        Key<Integer> k2 = JKeyType.IntKey().make("expTime", JUnits.second);
        Key<Integer> k2bad = JKeyType.IntKey().make("missingKey");
        Key<Integer> k3 = JKeyType.IntKey().make("filter");
        Key<UTCTime> k4 = JKeyType.UTCTimeKey().make("creation-time");

        //prefix
        Prefix prefix = Prefix.apply(JSubsystem.WFOS, "red.detector");

        //parameters
        Boolean[] boolArray = {true, false, true, false};
        Parameter<Boolean> i1 = k1.setAll(boolArray);
        Parameter<Integer> i2 = k2.set(1, 2, 3, 4);

        //create Observe, add sequentially using add
        Observe oc1 = new Observe(prefix, new CommandName("move"), Optional.of(obsId)).add(i1).add(i2);

        //access parameters
        Optional<Parameter<Boolean>> k1Param = oc1.jGet(k1); //present
        java.util.List<Boolean> values = k1Param.orElseThrow().jValues();

        //access parameters
        Optional<Parameter<ArrayData<Float>>> k2BadParam = oc1.jGet(k2bad.keyName(), JKeyType.FloatArrayKey());

        //add more than one parameters, using madd
        Observe oc2 = oc1.madd(k3.set(1, 2, 3, 4).withUnits(JUnits.day), k4.set(UTCTime.now()));
        int paramSize = oc2.size();

        //update existing key with set
        Integer[] intArray = {5, 6, 7, 8};
        Observe oc3 = oc1.add(k2.setAll(intArray));

        //remove a key
        Observe oc4 = oc2.remove(k4);

        //list all keys
        java.util.List<String> allKeys = oc4.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());
        //#observe

        //validations
        Assert.assertTrue(k1Param.isPresent());
        Assert.assertFalse(k2BadParam.isPresent());
        Assert.assertEquals(4, paramSize);
        Assert.assertArrayEquals(boolArray, values.toArray());
        Assert.assertArrayEquals(intArray, (Integer[]) oc3.jGet(k2).orElseThrow().values());
        Assert.assertEquals(3, oc4.size());
    }

    @Test
    public void showUsageOfWaitCommand() {
        //#wait
        //keys
        Key<Boolean> k1 = JKeyType.BooleanKey().make("repeat");
        Key<Integer> k2 = JKeyType.IntKey().make("expTime", JUnits.second);
        Key<Integer> k2bad = JKeyType.IntKey().make("missingKey");
        Key<Integer> k3 = JKeyType.IntKey().make("filter");
        Key<UTCTime> k4 = JKeyType.UTCTimeKey().make("creation-time");

        //prefix
        Prefix prefix = Prefix.apply(JSubsystem.WFOS, "red.detector");

        //parameters
        Boolean[] boolArray = {true, false, true, false};
        Parameter<Boolean> i1 = k1.setAll(boolArray);
        Parameter<Integer> i2 = k2.set(1, 2, 3, 4);

        //create Wait, add sequentially using add
        Wait wc1 = new Wait(prefix, new CommandName("move"), Optional.of(obsId)).add(i1).add(i2);

        //access parameters using jGet
        Optional<Parameter<Boolean>> k1Param = wc1.jGet(k1); //present
        java.util.List<Boolean> values = k1Param.orElseThrow().jValues();

        //access parameters
        Optional<Parameter<ArrayData<Float>>> k2BadParam = wc1.jGet("absentKeyHere", JKeyType.FloatArrayKey());

        //add more than one parameters, using madd
        Wait wc2 = wc1.madd(k3.set(1, 2, 3, 4).withUnits(JUnits.day), k4.set(UTCTime.now()));
        int paramSize = wc2.size();

        //update existing key with set
        Integer[] intArray = {5, 6, 7, 8};
        Wait wc3 = wc1.add(k2.setAll(intArray));

        //remove a key
        Wait wc4 = wc2.remove(k4);

        //list all keys
        java.util.List<String> allKeys = wc4.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());
        //#wait

        //validations
        Assert.assertTrue(k1Param.isPresent());
        Assert.assertFalse(k2BadParam.isPresent());
        Assert.assertEquals(4, paramSize);
        Assert.assertArrayEquals(boolArray, values.toArray());
        Assert.assertArrayEquals(intArray, (Integer[]) wc3.jGet(k2).orElseThrow().values());
        Assert.assertEquals(3, wc4.size());
    }

    @Test
    public void showJsonSerialization() {
        //#json-serialization
        //key
        Key<MatrixData<Double>> k1 = JKeyType.DoubleMatrixKey().make("myMatrix");

        //values
        Double[][] doubles = {{1.0, 2.0, 3.0}, {4.1, 5.1, 6.1}, {7.2, 8.2, 9.2}};
        MatrixData<Double> m1 = MatrixData.fromArrays(doubles);

        //parameter
        Parameter<MatrixData<Double>> i1 = k1.set(m1);

        Prefix prefix = Prefix.apply(JSubsystem.WFOS, "blue.filter");

        //commands
        Setup sc = new Setup(prefix, new CommandName("move"), Optional.of(obsId)).add(i1);
        Observe oc = new Observe(prefix, new CommandName("move"), Optional.of(obsId)).add(i1);
        Wait wc = new Wait(prefix, new CommandName("move"), Optional.of(obsId)).add(i1);

        //json support - write
        JsValue scJson = JavaJsonSupport.writeSequenceCommand(sc);
        JsValue ocJson = JavaJsonSupport.writeSequenceCommand(oc);
        JsValue wcJson = JavaJsonSupport.writeSequenceCommand(wc);

        //optionally prettify
        String str = Json.prettyPrint(scJson);

        //construct command from string
        Setup sc1 = JavaJsonSupport.readSequenceCommand(Json.parse(str));
        Observe oc1 = JavaJsonSupport.readSequenceCommand(ocJson);
        Wait wc1 = JavaJsonSupport.readSequenceCommand(wcJson);
        //#json-serialization

        Assert.assertEquals(sc, sc1);
        Assert.assertEquals(oc, oc1);
        Assert.assertEquals(wc, wc1);
    }

    @Test
    public void showUniqueKeyConstraintExample() {
        //#unique-key
        //keys
        Key<Integer> encoderKey = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Key<Integer> filterKey = JKeyType.IntKey().make("filter");
        Key<Integer> miscKey = JKeyType.IntKey().make("misc.");

        //prefix
        Prefix prefix = Prefix.apply(JSubsystem.WFOS, "blue.filter");

        //params
        Parameter<Integer> encParam1 = encoderKey.set(1);
        Parameter<Integer> encParam2 = encoderKey.set(2);
        Parameter<Integer> encParam3 = encoderKey.set(3);

        Parameter<Integer> filterParam1 = filterKey.set(1);
        Parameter<Integer> filterParam2 = filterKey.set(2);
        Parameter<Integer> filterParam3 = filterKey.set(3);

        Parameter<Integer> miscParam1 = miscKey.set(100);

        //Setup command with duplicate key via madd
        Setup setup = new Setup(prefix, new CommandName("move"), Optional.of(obsId)).madd(
                encParam1,
                encParam2,
                encParam3,
                filterParam1,
                filterParam2,
                filterParam3);
        //four duplicate keys are removed; now contains one Encoder and one Filter key
        Set<String> uniqueKeys1 = setup.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toUnmodifiableSet());

        //try adding duplicate keys via add + madd
        Setup changedSetup = setup.add(encParam3).madd(filterParam1, filterParam2, filterParam3);
        //duplicate keys will not be added. Should contain one Encoder and one Filter key
        Set<String> uniqueKeys2 = changedSetup.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toUnmodifiableSet());

        //miscKey(unique) will be added; encoderKey(duplicate) will not be added
        Setup finalSetUp = setup.madd(miscParam1, encParam1);
        //now contains encoderKey, filterKey, miscKey
        Set<String> uniqueKeys3 = finalSetUp.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toUnmodifiableSet());
        //#unique-key

        //validations
        Assert.assertEquals(uniqueKeys1, Set.of(encoderKey.keyName(), filterKey.keyName()));
        Assert.assertEquals(uniqueKeys2, Set.of(encoderKey.keyName(), filterKey.keyName()));
        Assert.assertEquals(uniqueKeys3, Set.of(encoderKey.keyName(), filterKey.keyName(), miscKey.keyName()));
    }
/*
    @Test
    public void showCloneCommandExample() {
        Prefix prefix = Prefix.apply(JSubsystem.WFOS, "blue.filter");
        Key<Integer> encoderIntKey = JKeyType.IntKey().make("encoder");
        Parameter<Integer> encoderParam = encoderIntKey.set(22, 33);
        CommandName commandName = new CommandName("move");

        //#clone-command
        Setup setup = new Setup(prefix, commandName, Optional.of(obsId)).add(encoderParam);
        Setup setup2 = setup.cloneCommand();

        Observe observe = new Observe(prefix, commandName, Optional.empty()).add(encoderParam);
        Observe observe2 = observe.cloneCommand();

        Wait wait = new Wait(prefix, commandName, Optional.of(obsId)).add(encoderParam);
        Wait wait2 = wait.cloneCommand();
        //#clone-command

        Assert.assertNotEquals(setup.runId(), setup2.runId());
        Assert.assertEquals(setup.commandName(), setup2.commandName());
        Assert.assertEquals(setup.jMaybeObsId(), setup2.jMaybeObsId());
        Assert.assertEquals(setup.jParamSet(), setup2.jParamSet());
        Assert.assertEquals(setup.source(), setup2.source());

        Assert.assertNotEquals(observe.runId(), observe2.runId());
        Assert.assertEquals(observe.commandName(), observe2.commandName());
        Assert.assertEquals(observe.jMaybeObsId(), observe2.jMaybeObsId());
        Assert.assertEquals(observe.jParamSet(), observe2.jParamSet());
        Assert.assertEquals(observe.source(), observe2.source());

        Assert.assertNotEquals(wait.runId(), wait2.runId());
        Assert.assertEquals(wait.commandName(), wait2.commandName());
        Assert.assertEquals(wait.jMaybeObsId(), wait2.jMaybeObsId());
        Assert.assertEquals(wait.jParamSet(), wait2.jParamSet());
        Assert.assertEquals(wait.source(), wait2.source());
    }
 */
}
