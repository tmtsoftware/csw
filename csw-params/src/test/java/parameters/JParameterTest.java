package parameters;

import csw.param.JsonSupport;
import csw.param.parameters.*;
import org.junit.Assert;
import org.junit.Test;
import spray.json.JsValue;

import java.util.Vector;

public class JParameterTest {

    @Test
    public void testBooleanParameter() {
        String encoder = "encoder";

        GKey<Boolean> key = JKeys.BooleanKey().make(encoder);
//        key.gset()
        GParam<Boolean> param = key.set(true, false);

        Vector<Boolean> booleanVector = new Vector<Boolean>();
        booleanVector.add(true);
        booleanVector.add(false);

//        GParam<Boolean> param2 = key.gset(booleanVector, UnitsOfMeasure.NoUnits$.MODULE$);

        JsValue jsValue = param.toJson();

        GParam<Boolean> booleanGParam = jsValue.convertTo(JsonSupport.dd());

        Assert.assertEquals(param, booleanGParam);
//        Assert.assertNotEquals(param2, booleanGParam);
    }

    @Test
    public void testSet() {

    }
}