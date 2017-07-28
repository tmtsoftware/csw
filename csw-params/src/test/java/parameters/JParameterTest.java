package parameters;

import csw.param.JavaHelpers;
import csw.param.UnitsOfMeasure;
import csw.param.parameters.GParam;
import csw.param.parameters.JKeys;
import csw.param.parameters.Keys;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class JParameterTest {

    @Test
    public void testBooleanParameter() {
        String encoder = "encoder";
//        BooleanKey encoderKey = BooleanKey.apply(encoder);

//        JKeys.Integer key = new Keys.Integer("aa");
//        Keys.Integer key2 = new Keys.Integer("aa");
//        Integer[] i = {1,2,3};
//        GParam<Integer> gset = key.gset(i, UnitsOfMeasure.NoUnits$.MODULE$);
//        GParam<Object> gset2 = key2.gset(i, UnitsOfMeasure.NoUnits$.MODULE$);
//        GParam<Integer> gset = key.set(i, UnitsOfMeasure.NoUnits$.MODULE$);
//        Object value1 = gset.value(0);
//        System.out.println(value1);
//        System.out.println(gset.toJson());




        List<Boolean> params = Arrays.asList(true, false);
//        BooleanParameter p1 = JavaHelpers.jset(encoderKey, params, UnitsOfMeasure.NoUnits$.MODULE$);
//
//        Assert.assertEquals(params.get(0), JavaHelpers.jget(p1, 0).get());
//        Assert.assertEquals(params.get(1), JavaHelpers.jget(p1, 1).get());
//        Assert.assertEquals(encoder, p1.keyName());
//        Assert.assertEquals(UnitsOfMeasure.NoUnits$.MODULE$, p1.units());
    }
}