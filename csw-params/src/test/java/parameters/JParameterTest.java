package parameters;

import csw.param.TestJavaFormats;
import csw.param.parameters.GArray;
import csw.param.parameters.GKey;
import csw.param.parameters.GParam;
import csw.param.parameters.JKeys;
import org.junit.Assert;
import org.junit.Test;
import spray.json.JsValue;

import java.util.Vector;

public class JParameterTest {

    @Test
    public void testBooleanParameter() {
        String encoder = "encoder";

        GKey<Boolean> key = JKeys.BooleanKey().make(encoder);
        GParam<Boolean> param = key.set(true, false);

        Vector<Boolean> booleanVector = new Vector<Boolean>();
        booleanVector.add(true);
        booleanVector.add(false);

        JsValue jsValue = param.toJson();
        GParam<Boolean> booleanGParam = jsValue.convertTo(TestJavaFormats.dd());
        Assert.assertEquals(param, booleanGParam);
    }

    @Test
    public void testSet() {
        Integer[] array = {1, 2, 3};

        GArray<Integer> gArray = GArray.fromArray(array);
        GKey<GArray<Integer>> arrayKey = JKeys.IntegerArrayKey().make("arrayKey");
        GParam<GArray<Integer>> arrayGParam = arrayKey.set(gArray);

//        Assert.assertEquals(arrayGParam.values(),
    }
}
