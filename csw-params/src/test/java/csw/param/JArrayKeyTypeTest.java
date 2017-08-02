package csw.param;

import csw.param.models.ArrayData;
import csw.param.models.JArrayData;
import csw.param.parameters.JKeyTypes;
import csw.param.parameters.Key;
import csw.param.parameters.Parameter;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JArrayKeyTypeTest {

    @Test
    public void testShortArrayKeyParameter() {
        String keyName = "shortKey";
        Key<ArrayData<Short>> key = JKeyTypes.ShortArrayKey().make(keyName);
        Short[] shortArray1 = {10, 20, 30};
        Short[] shortArray2 = {100, 200, 300, 400};

        ArrayData<Short> paramArrayData1 = JArrayData.fromArray(shortArray1);
        ArrayData<Short> paramArrayData2 = JArrayData.fromArray(shortArray2);

        // key.set without Units
        ArrayData<Short>[] arrayData = new ArrayData[]{paramArrayData1, paramArrayData2};
        Parameter<ArrayData<Short>> parameterWithoutUnits = key.set(arrayData);

        List<ArrayData<Short>> paramValuesAsList = parameterWithoutUnits.jValues();
        ArrayData<Short>[] paramValues = (ArrayData<Short>[]) parameterWithoutUnits.values();

        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.ShortArrayKey(),key.keyType());
        Assert.assertEquals(arrayData.length, paramValuesAsList.size());
        Assert.assertEquals(arrayData.length, paramValues.length);
        Assert.assertEquals(paramArrayData1, paramValues[0]);
        Assert.assertEquals(paramArrayData2, paramValues[1]);
        Assert.assertEquals(paramArrayData1, paramValuesAsList.get(0));
        Assert.assertEquals(paramArrayData2, paramValuesAsList.get(1));
        Assert.assertEquals(paramArrayData1, parameterWithoutUnits.head());
        Assert.assertEquals(UnitsOfMeasure.NoUnits$.MODULE$, parameterWithoutUnits.units());

        UnitsOfMeasure.degrees$ degreesUnit = UnitsOfMeasure.degrees$.MODULE$;
        // key.set with Units
        Parameter<ArrayData<Short>> parameterWithUnits = key.set(arrayData, degreesUnit);
        List<ArrayData<Short>> paramValuesAsList2 = parameterWithoutUnits.jValues();
        ArrayData<Short>[] paramValues2 = (ArrayData<Short>[]) parameterWithoutUnits.values();

        Assert.assertEquals(degreesUnit, parameterWithUnits.units());
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.ShortArrayKey(),key.keyType());
        Assert.assertEquals(arrayData.length, paramValuesAsList2.size());
        Assert.assertEquals(arrayData.length, paramValues2.length);
        Assert.assertEquals(paramArrayData1, paramValues2[0]);
        Assert.assertEquals(paramArrayData2, paramValues2[1]);
        Assert.assertEquals(paramArrayData1, paramValuesAsList2.get(0));
        Assert.assertEquals(paramArrayData2, paramValuesAsList2.get(1));
        Assert.assertEquals(paramArrayData1, parameterWithoutUnits.head());
    }

}
