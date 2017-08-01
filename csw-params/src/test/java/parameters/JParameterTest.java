package parameters;

import csw.param.TestJavaFormats;
import csw.param.models.ArrayData;
import csw.param.parameters.*;
import org.junit.Assert;
import org.junit.Test;
import spray.json.JsValue;

import java.util.Vector;

public class JParameterTest {

    @Test
    public void testBooleanParameter() {
        String encoder = "encoder";

        Key<Boolean> key = JKeyTypes.BooleanKey().make(encoder);
        Parameter<Boolean> parameter = key.set(true, false);

        Vector<Boolean> booleanVector = new Vector<Boolean>();
        booleanVector.add(true);
        booleanVector.add(false);

        JsValue jsValue = parameter.toJson();
        Parameter<Boolean> booleanParameter = jsValue.convertTo(TestJavaFormats.dd());
        Assert.assertEquals(parameter, booleanParameter);
    }

    @Test
    public void testSet() {
        Integer[] array = {1, 2, 3};

        ArrayData<Integer> arrayData = ArrayData.fromArray(array);
        Key<ArrayData<Integer>> arrayKey = JKeyTypes.IntArrayKey().make("arrayKey");
        Parameter<ArrayData<Integer>> arrayParameter = arrayKey.set(arrayData);

//        Assert.assertEquals(arrayGParam.values(),
    }
}
