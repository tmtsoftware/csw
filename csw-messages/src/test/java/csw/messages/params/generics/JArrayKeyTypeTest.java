package csw.messages.params.generics;

import csw.messages.params.models.ArrayData;
import csw.messages.params.models.Units;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static csw.messages.javadsl.JUnits.*;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-190: Implement Unit Support
// DEOPSCSW-184: Change configurations - attributes and values
public class JArrayKeyTypeTest {

    private final Units NoUnit = NoUnits;

    private void commonAssertions(String keyName, KeyType keyType, ArrayData[] testData, Parameter parameter, Units unit) {
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

    // DEOPSCSW-186: Binary value payload
    @Test
    public void testByteArrayKeyParameter() {
        String keyName = "ByteKey";
        Key<ArrayData<Byte>> key = JKeyType.ByteArrayKey().make(keyName);
        Byte[] byteArray1 = {1, 2, 3};
        Byte[] byteArray2 = {4, 5, 6, 7};

        ArrayData<Byte> data1 = ArrayData.fromJavaArray(byteArray1);
        ArrayData<Byte> data2 = ArrayData.fromJavaArray(byteArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Byte>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.ByteArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Byte>> parameterWithUnits = key.set(arrayData, millisecond);
        commonAssertions(keyName, JKeyType.ByteArrayKey(), arrayData, parameterWithUnits, millisecond);
    }

    @Test
    public void testShortArrayKeyParameter() {
        String keyName = "shortKey";
        Key<ArrayData<Short>> key = JKeyType.ShortArrayKey().make(keyName);
        Short[] shortArray1 = {10, 20, 30};
        Short[] shortArray2 = {100, 200, 300, 400};

        ArrayData<Short> data1 = ArrayData.fromJavaArray(shortArray1);
        ArrayData<Short> data2 = ArrayData.fromJavaArray(shortArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Short>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.ShortArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Short>> parameterWithUnits = key.set(arrayData, degree);
        commonAssertions(keyName, JKeyType.ShortArrayKey(), arrayData, parameterWithUnits, degree);
    }

    @Test
    public void testLongArrayKeyParameter() {
        String keyName = "longKey";
        Key<ArrayData<Long>> key = JKeyType.LongArrayKey().make(keyName);
        Long[] longArray1 = {100L, 200L, 300L};
        Long[] longArray2 = {400L, 500L, 600L, 700L};

        ArrayData<Long> data1 = ArrayData.fromJavaArray(longArray1);
        ArrayData<Long> data2 = ArrayData.fromJavaArray(longArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Long>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.LongArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Long>> parameterWithUnits = key.set(arrayData, millimeter);
        commonAssertions(keyName, JKeyType.LongArrayKey(), arrayData, parameterWithUnits, millimeter);
    }

    @Test
    public void testIntArrayKeyParameter() {
        String keyName = "integerKey";
        Key<ArrayData<Integer>> key = JKeyType.IntArrayKey().make(keyName);
        Integer[] integerArray1 = {100, 200, 300};
        Integer[] integerArray2 = {400, 500, 600, 700};

        ArrayData<Integer> data1 = ArrayData.fromJavaArray(integerArray1);
        ArrayData<Integer> data2 = ArrayData.fromJavaArray(integerArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Integer>> parameterWithoutUnits = key.set(data1, data2);

        commonAssertions(keyName, JKeyType.IntArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Integer>> parameterWithUnits = key.set(arrayData, kilometer);
        commonAssertions(keyName, JKeyType.IntArrayKey(), arrayData, parameterWithUnits, kilometer);
    }

    @Test
    public void testFloatArrayKeyParameter() {
        String keyName = "floatKey";
        Key<ArrayData<Float>> key = JKeyType.FloatArrayKey().make(keyName);
        Float[] floatArray1 = {100f, 200f, 300f};
        Float[] floatArray2 = {400f, 500f, 600f, 700f};

        ArrayData<Float> data1 = ArrayData.fromJavaArray(floatArray1);
        ArrayData<Float> data2 = ArrayData.fromJavaArray(floatArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Float>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.FloatArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Float>> parameterWithUnits = key.set(arrayData, meter);
        commonAssertions(keyName, JKeyType.FloatArrayKey(), arrayData, parameterWithUnits, meter);
    }

    @Test
    public void testDoubleArrayKeyParameter() {
        String keyName = "doubleKey";
        Key<ArrayData<Double>> key = JKeyType.DoubleArrayKey().make(keyName);
        Double[] doubleArray1 = {100d, 200d, 300d};
        Double[] doubleArray2 = {400d, 500d, 600d, 700d};

        ArrayData<Double> data1 = ArrayData.fromJavaArray(doubleArray1);
        ArrayData<Double> data2 = ArrayData.fromJavaArray(doubleArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Double>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.DoubleArrayKey(), arrayData, parameterWithoutUnits, NoUnit);

        // key.set with Units
        Parameter<ArrayData<Double>> parameterWithUnits = key.set(arrayData, encoder);
        commonAssertions(keyName, JKeyType.DoubleArrayKey(), arrayData, parameterWithUnits, encoder);
    }
}
