package csw.param.parameters;

import csw.param.models.ArrayData;
import csw.param.models.JArrayData;
import csw.units.Units;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static csw.param.javadsl.JUnits.*;

// DEOPSCSW-190: Implement Unit Support
public class JArrayKeyTypeTest {

    private final Units NoUnit = NoUnits;

    void commonAssertions(String keyName, KeyType keyType, ArrayData[] testData, Parameter parameter, Units unit) {
        Assert.assertEquals(keyName, keyName);
        Assert.assertEquals(keyType,parameter.keyType());

        List paramValuesAsList = parameter.jValues();
        Assert.assertEquals(testData.length, paramValuesAsList.size());
        Assert.assertEquals(testData[0], paramValuesAsList.get(0));
        Assert.assertEquals(testData[1], paramValuesAsList.get(1));

        Assert.assertEquals(testData[0], parameter.head());
        Assert.assertEquals(unit, parameter.units());

        Object[] paramValuesAsArray = (Object[]) parameter.values();
        Assert.assertEquals(testData.length, paramValuesAsArray.length);
        Assert.assertEquals(testData[0], paramValuesAsArray[0]);
        Assert.assertEquals(testData[1], paramValuesAsArray[1]);
    }

    @Test
    public void testByteArrayKeyParameter() {
        String keyName = "ByteKey";
        Key<ArrayData<Byte>> key = JKeyTypes.ByteArrayKey().make(keyName);
        Byte[] byteArray1 = {1, 2, 3};
        Byte[] byteArray2 = {4, 5, 6, 7};

        ArrayData<Byte> data1 = JArrayData.fromArray(byteArray1);
        ArrayData<Byte> data2 = JArrayData.fromArray(byteArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Byte>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyTypes.ByteArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Byte>> parameterWithUnits = key.set(arrayData, milliseconds);
        commonAssertions(keyName, JKeyTypes.ByteArrayKey(), arrayData, parameterWithUnits, milliseconds);
    }
    
    @Test
    public void testShortArrayKeyParameter() {
        String keyName = "shortKey";
        Key<ArrayData<Short>> key = JKeyTypes.ShortArrayKey().make(keyName);
        Short[] shortArray1 = {10, 20, 30};
        Short[] shortArray2 = {100, 200, 300, 400};

        ArrayData<Short> data1 = JArrayData.fromArray(shortArray1);
        ArrayData<Short> data2 = JArrayData.fromArray(shortArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Short>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyTypes.ShortArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Short>> parameterWithUnits = key.set(arrayData, degrees);
        commonAssertions(keyName, JKeyTypes.ShortArrayKey(), arrayData, parameterWithUnits, degrees);
    }

    @Test
    public void testLongArrayKeyParameter() {
        String keyName = "longKey";
        Key<ArrayData<Long>> key = JKeyTypes.LongArrayKey().make(keyName);
        Long[] longArray1 = {100L, 200L, 300L};
        Long[] longArray2 = {400L, 500L, 600L, 700L};

        ArrayData<Long> data1 = JArrayData.fromArray(longArray1);
        ArrayData<Long> data2 = JArrayData.fromArray(longArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Long>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyTypes.LongArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Long>> parameterWithUnits = key.set(arrayData, millimeters);
        commonAssertions(keyName, JKeyTypes.LongArrayKey(), arrayData, parameterWithUnits, millimeters);
    }
    
    @Test
    public void testIntArrayKeyParameter() {
        String keyName = "integerKey";
        Key<ArrayData<Integer>> key = JKeyTypes.IntArrayKey().make(keyName);
        Integer[] integerArray1 = {100, 200, 300};
        Integer[] integerArray2 = {400, 500, 600, 700};

        ArrayData<Integer> data1 = JArrayData.fromArray(integerArray1);
        ArrayData<Integer> data2 = JArrayData.fromArray(integerArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Integer>> parameterWithoutUnits = key.set(data1, data2);

        commonAssertions(keyName, JKeyTypes.IntArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Integer>> parameterWithUnits = key.set(arrayData, kilometers);
        commonAssertions(keyName, JKeyTypes.IntArrayKey(), arrayData, parameterWithUnits, kilometers);
    }

    @Test
    public void testFloatArrayKeyParameter() {
        String keyName = "floatKey";
        Key<ArrayData<Float>> key = JKeyTypes.FloatArrayKey().make(keyName);
        Float[] floatArray1 = {100f, 200f, 300f};
        Float[] floatArray2 = {400f, 500f, 600f, 700f};

        ArrayData<Float> data1 = JArrayData.fromArray(floatArray1);
        ArrayData<Float> data2 = JArrayData.fromArray(floatArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Float>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyTypes.FloatArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Float>> parameterWithUnits = key.set(arrayData, meters);
        commonAssertions(keyName, JKeyTypes.FloatArrayKey(), arrayData, parameterWithUnits, meters);
    }

    @Test
    public void testDoubleArrayKeyParameter() {
        String keyName = "doubleKey";
        Key<ArrayData<Double>> key = JKeyTypes.DoubleArrayKey().make(keyName);
        Double[] doubleArray1 = {100d, 200d, 300d};
        Double[] doubleArray2 = {400d, 500d, 600d, 700d};

        ArrayData<Double> data1 = JArrayData.fromArray(doubleArray1);
        ArrayData<Double> data2 = JArrayData.fromArray(doubleArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Double>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyTypes.DoubleArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Double>> parameterWithUnits = key.set(arrayData, encoder);
        commonAssertions(keyName, JKeyTypes.DoubleArrayKey(), arrayData, parameterWithUnits, encoder);
    }
}
