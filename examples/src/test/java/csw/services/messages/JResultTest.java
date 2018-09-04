package csw.services.messages;

import csw.messages.commands.Result;
import csw.messages.params.formats.JavaJsonSupport;
import csw.messages.params.generics.JKeyType;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.MatrixData;
import csw.messages.params.models.ObsId;
import csw.messages.params.models.Id;
import org.junit.Assert;
import org.junit.Test;
import play.api.libs.json.JsValue;
import play.api.libs.json.Json;

import java.util.*;
import java.util.stream.Collectors;

public class JResultTest {

    //#runid
    Id runId = Id.apply();
    //#runid

    @Test
    public void showUsageOfResult() {
        //#result
        //keys
        Key<Integer> k1 = JKeyType.IntKey().make("encoder");
        Key<Integer> k2 = JKeyType.IntKey().make("windspeed");
        Key<String> k3 = JKeyType.StringKey().make("filter");
        Key<Integer> k4 = JKeyType.IntKey().make("notUsed");

        //prefix
        String prefix = "wfos.prog.cloudcover";

        //#obsid
        ObsId obsId = new ObsId("Obs001");

        //parameters
        Parameter<Integer> p1 = k1.set(22);
        Parameter<Integer> p2 = k2.set(44);
        Parameter<String> p3 = k3.set("A", "B", "C", "D");

        //Create Result using madd
        Result r1 = new Result(prefix).madd(p1, p2);
        //Create Result using madd
        Result r2 = new Result(prefix).madd(p1, p2);
        //Create Result and use madd, add
        Result r3 = new Result(prefix).madd(p1, p2).add(p3);

        //access keys
        Boolean k1Exists = r1.exists(k1); //true

        //access Parameters
        Optional<Parameter<Integer>> p4 = r1.jGet(k1);

        //access values
        List<Integer> v1 = r1.jGet(k1).get().jValues();
        List<Integer> v2 = r2.parameter(k2).jValues();

        //k4 is missing
        Set<String> missingKeys = r3.jMissingKeys(k1, k2, k3, k4);

        //remove keys
        Result r4 = r3.remove(k3);
        //#result

        Assert.assertTrue(k1Exists);
        Assert.assertTrue(p4.get() == p1);
        Assert.assertEquals(new HashSet<>(v1), new HashSet<>(p1.jValues()));
        Assert.assertEquals(new HashSet<>(v2), new HashSet<>(p2.jValues()));
        Assert.assertEquals(new HashSet<>(missingKeys), new HashSet<>(Collections.singletonList(k4.keyName())));
        Assert.assertTrue(r2.equals(r4));
    }

    @Test
    public void showJsonSerialization() {
        //#json-serialization
        //key
        Key<MatrixData<Double>> k1 = JKeyType.DoubleMatrixKey().make("myMatrix");

        //values
        Double[][] doubles = {{1.0, 2.0, 3.0}, {4.1, 5.1, 6.1}, {7.2, 8.2, 9.2}};
        MatrixData<Double> m1 = MatrixData.fromJavaArrays(Double.class, doubles);

        //parameter
        Parameter<MatrixData<Double>> i1 = k1.set(m1);

        //ObsId
        ObsId obsId = new ObsId("Obs001");

        //prefix
        String prefix = "wfos.prog.cloudcover";

        //result
        Result result = new Result(prefix).add(i1);

        //json support - write
        JsValue resultJson = JavaJsonSupport.writeResult(result);

        //optionally prettify
        String str = Json.prettyPrint(resultJson);

        //construct result from string
        Result result1 = JavaJsonSupport.readResult(Json.parse(str));
        //#json-serialization

        Assert.assertTrue(result.equals(result1));
    }

    @Test
    public void showUniqueKeyConstraintExample() {
        //#unique-key
        //keys
        Key<Integer> encoderKey = JKeyType.IntKey().make("encoder");
        Key<Integer> filterKey = JKeyType.IntKey().make("filter");
        Key<Integer> miscKey = JKeyType.IntKey().make("misc.");

        //ObsId
        ObsId obsId = new ObsId("Obs001");

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

        //Setup command with duplicate key via madd
        Result result = new Result(prefix).madd(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3);
        //four duplicate keys are removed; now contains one Encoder and one Filter key
        List<String> uniqueKeys1 = result.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());

        //try adding duplicate keys via add + madd
        Result changedResult = result.add(encParam3).madd(filterParam1, filterParam2, filterParam3);
        //duplicate keys will not be added. Should contain one Encoder and one Filter key
        List<String> uniqueKeys2 = changedResult.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());

        //miscKey(unique) will be added; encoderKey(duplicate) will not be added
        Result finalResult = result.madd(miscParam1, encParam1);
        //now contains encoderKey, filterKey, miscKey
        List<String> uniqueKeys3 = finalResult.jParamSet().stream().map(Parameter::keyName).collect(Collectors.toList());
        //#unique-key

        Assert.assertEquals(new HashSet<>(uniqueKeys1), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName())));
        Assert.assertEquals(new HashSet<>(uniqueKeys2), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName())));
        Assert.assertEquals(new HashSet<>(uniqueKeys3), new HashSet<>(Arrays.asList(encoderKey.keyName(), filterKey.keyName(), miscKey.keyName())));
    }
}
