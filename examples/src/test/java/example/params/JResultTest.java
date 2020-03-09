package example.params;

import csw.params.commands.Result;
import csw.params.core.formats.JavaJsonSupport;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Id;
import csw.params.core.models.MatrixData;
import csw.params.core.models.ObsId;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.prefix.models.Prefix;
import csw.prefix.javadsl.JSubsystem;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import play.api.libs.json.JsValue;
import play.api.libs.json.Json;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JResultTest extends JUnitSuite {

    //#runid
    Id runId = Id.apply();
    //#runid

    @Test
    public void showUsageOfResult() {
        //#result
        //keys
        Key<Integer> k1 = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Key<Integer> k2 = JKeyType.IntKey().make("windspeed", JUnits.NoUnits);
        Key<String> k3 = JKeyType.StringKey().make("filter", JUnits.NoUnits);
        Key<Integer> k4 = JKeyType.IntKey().make("notUsed", JUnits.NoUnits);

        //parameters
        Parameter<Integer> p1 = k1.set(22);
        Parameter<Integer> p2 = k2.set(44);
        Parameter<String> p3 = k3.set("A", "B", "C", "D");

        //Create Result using madd
        Result r1 = new Result().madd(p1, p2);
        //Create Result in line
        Result r2 = new Result().madd(p1, p2);
        //Create Result and use madd, add
        Result r3 = new Result().madd(p1, p2).add(p3);

        //access keys
        boolean k1Exists = r1.exists(k1); //true

        //access Parameters
        Optional<Parameter<Integer>> p4 = r1.jGet(k1);

        //access values
        List<Integer> v1 = r1.jGet(k1).orElseThrow().jValues();
        List<Integer> v2 = r2.parameter(k2).jValues();

        //k4 is missing
        Set<String> missingKeys = r3.jMissingKeys(k1, k2, k3, k4);

        //remove keys
        Result r4 = r3.remove(k3);
        //#result

        Assert.assertTrue(k1Exists);
        Assert.assertSame(p4.orElseThrow(), p1);
        Assert.assertEquals(Set.copyOf(v1), Set.copyOf(p1.jValues()));
        Assert.assertEquals(Set.copyOf(v2), Set.copyOf(p2.jValues()));
        Assert.assertEquals(missingKeys, Set.of(k4.keyName()));
        Assert.assertEquals(r2, r4);
    }

    @Test
    public void showJsonSerialization() {
        //#json-serialization
        //key
        Key<MatrixData<Double>> k1 = JKeyType.DoubleMatrixKey().make("myMatrix", JUnits.NoUnits);

        //values
        Double[][] doubles = {{1.0, 2.0, 3.0}, {4.1, 5.1, 6.1}, {7.2, 8.2, 9.2}};
        MatrixData<Double> m1 = MatrixData.fromArrays(doubles);

        //parameter
        Parameter<MatrixData<Double>> i1 = k1.set(m1);

        //ObsId
        ObsId obsId = new ObsId("Obs001");

        //prefix
        Prefix prefix = Prefix.apply(JSubsystem.WFOS, "prog.cloudcover");

        //result
        Result result = new Result().add(i1);

        //json support - write
        JsValue resultJson = JavaJsonSupport.writeResult(result);

        //optionally prettify
        String str = Json.prettyPrint(resultJson);

        //construct result from string
        Result result1 = JavaJsonSupport.readResult(Json.parse(str));
        //#json-serialization

        Assert.assertEquals(result, result1);
    }

    @Test
    public void showUniqueKeyConstraintExample() {
        //#unique-key
        //keys
        Key<Integer> encoderKey = JKeyType.IntKey().make("encoder", JUnits.encoder);
        Key<Integer> filterKey = JKeyType.IntKey().make("filter", JUnits.NoUnits);
        Key<Integer> miscKey = JKeyType.IntKey().make("misc.", JUnits.NoUnits);

        //ObsId
        ObsId obsId = new ObsId("Obs001");

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
        Result result = new Result().madd(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3);
        //four duplicate keys are removed; now contains one Encoder and one Filter key
        Set<String> uniqueKeys1 = result.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toUnmodifiableSet());

        //try adding duplicate keys via add + madd
        Result changedResult = result.add(encParam3).madd(filterParam1, filterParam2, filterParam3);
        //duplicate keys will not be added. Should contain one Encoder and one Filter key
        Set<String> uniqueKeys2 = changedResult.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toUnmodifiableSet());

        //miscKey(unique) will be added; encoderKey(duplicate) will not be added
        Result finalResult = result.madd(miscParam1, encParam1);
        //now contains encoderKey, filterKey, miscKey
        Set<String> uniqueKeys3 = finalResult.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toUnmodifiableSet());
        //#unique-key

        Assert.assertEquals(uniqueKeys1, Set.of(encoderKey.keyName(), filterKey.keyName()));
        Assert.assertEquals(uniqueKeys2, Set.of(encoderKey.keyName(), filterKey.keyName()));
        Assert.assertEquals(uniqueKeys3, Set.of(encoderKey.keyName(), filterKey.keyName(), miscKey.keyName()));
    }
}
