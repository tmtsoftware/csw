package csw.params.core.generics;

import csw.params.javadsl.JKeyType;
import csw.params.core.models.ArrayData;
import csw.params.core.models.Units;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.List;

import static csw.params.javadsl.JUnits.*;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-190: Implement Unit Support
// DEOPSCSW-184: Change configurations - attributes and values
public class JArrayKeyTypeTest extends JUnitSuite {

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
    public void testByteArrayKeyParameter__DEOPSCSW_183_DEOPSCSW_190_DEOPSCSW_184_DEOPSCSW_186() {
        String keyName = "ByteKey";
        Key<ArrayData<Byte>> key = JKeyType.ByteArrayKey().make(keyName, NoUnits);
        Key<ArrayData<Byte>> keyUnits = JKeyType.ByteArrayKey().make(keyName, millisecond);
        Byte[] byteArray1 = {1, 2, 3};
        Byte[] byteArray2 = {4, 5, 6, 7};

        ArrayData<Byte> data1 = ArrayData.fromArray(byteArray1);
        ArrayData<Byte> data2 = ArrayData.fromArray(byteArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Byte>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.ByteArrayKey(), arrayData, parameterWithoutUnits, NoUnits);

        // key.set with Units
        Parameter<ArrayData<Byte>> parameterWithUnits = keyUnits.setAll(arrayData);
        commonAssertions(keyName, JKeyType.ByteArrayKey(), arrayData, parameterWithUnits, millisecond);
    }

    @Test
    public void testShortArrayKeyParameter__DEOPSCSW_183_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "shortKey";
        Key<ArrayData<Short>> key = JKeyType.ShortArrayKey().make(keyName, NoUnits);
        Key<ArrayData<Short>> keyUnits = JKeyType.ShortArrayKey().make(keyName, degree);
        Short[] shortArray1 = {10, 20, 30};
        Short[] shortArray2 = {100, 200, 300, 400};

        ArrayData<Short> data1 = ArrayData.fromArray(shortArray1);
        ArrayData<Short> data2 = ArrayData.fromArray(shortArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Short>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.ShortArrayKey(), arrayData, parameterWithoutUnits, NoUnits);

        // key.set with Units
        Parameter<ArrayData<Short>> parameterWithUnits = keyUnits.setAll(arrayData);
        commonAssertions(keyName, JKeyType.ShortArrayKey(), arrayData, parameterWithUnits, degree);
    }

    @Test
    public void testLongArrayKeyParameter__DEOPSCSW_183_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "longKey";
        Key<ArrayData<Long>> key = JKeyType.LongArrayKey().make(keyName, NoUnits);
        Key<ArrayData<Long>> keyUnits = JKeyType.LongArrayKey().make(keyName, millimeter);
        Long[] longArray1 = {100L, 200L, 300L};
        Long[] longArray2 = {400L, 500L, 600L, 700L};

        ArrayData<Long> data1 = ArrayData.fromArray(longArray1);
        ArrayData<Long> data2 = ArrayData.fromArray(longArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Long>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.LongArrayKey(), arrayData, parameterWithoutUnits, NoUnits);

        // key.set with Units
        Parameter<ArrayData<Long>> parameterWithUnits = keyUnits.setAll(arrayData);
        commonAssertions(keyName, JKeyType.LongArrayKey(), arrayData, parameterWithUnits, millimeter);
    }

    @Test
    public void testIntArrayKeyParameter__DEOPSCSW_183_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "integerKey";
        Key<ArrayData<Integer>> key = JKeyType.IntArrayKey().make(keyName, NoUnits);
        Key<ArrayData<Integer>> keyUnits = JKeyType.IntArrayKey().make(keyName, kilometer);
        Integer[] integerArray1 = {100, 200, 300};
        Integer[] integerArray2 = {400, 500, 600, 700};

        ArrayData<Integer> data1 = ArrayData.fromArray(integerArray1);
        ArrayData<Integer> data2 = ArrayData.fromArray(integerArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Integer>> parameterWithoutUnits = key.set(data1, data2);

        commonAssertions(keyName, JKeyType.IntArrayKey(), arrayData, parameterWithoutUnits, NoUnits);

        // key.set with Units
        Parameter<ArrayData<Integer>> parameterWithUnits = keyUnits.setAll(arrayData);
        commonAssertions(keyName, JKeyType.IntArrayKey(), arrayData, parameterWithUnits, kilometer);
    }

    @Test
    public void testFloatArrayKeyParameter__DEOPSCSW_183_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "floatKey";
        Key<ArrayData<Float>> key = JKeyType.FloatArrayKey().make(keyName, NoUnits);
        Key<ArrayData<Float>> keyUnits = JKeyType.FloatArrayKey().make(keyName, meter);
        Float[] floatArray1 = {100f, 200f, 300f};
        Float[] floatArray2 = {400f, 500f, 600f, 700f};

        ArrayData<Float> data1 = ArrayData.fromArray(floatArray1);
        ArrayData<Float> data2 = ArrayData.fromArray(floatArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Float>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.FloatArrayKey(), arrayData, parameterWithoutUnits, NoUnits);

        // key.set with Units
        Parameter<ArrayData<Float>> parameterWithUnits = keyUnits.setAll(arrayData);
        commonAssertions(keyName, JKeyType.FloatArrayKey(), arrayData, parameterWithUnits, meter);
    }

    @Test
    public void testDoubleArrayKeyParameter__DEOPSCSW_183_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "doubleKey";
        Key<ArrayData<Double>> key = JKeyType.DoubleArrayKey().make(keyName, NoUnits);
        Key<ArrayData<Double>> keyUnits = JKeyType.DoubleArrayKey().make(keyName, encoder);
        Double[] doubleArray1 = {100d, 200d, 300d};
        Double[] doubleArray2 = {400d, 500d, 600d, 700d};

        ArrayData<Double> data1 = ArrayData.fromArray(doubleArray1);
        ArrayData<Double> data2 = ArrayData.fromArray(doubleArray2);

        // key.set without Units
        ArrayData[] arrayData = new ArrayData[]{data1, data2};
        Parameter<ArrayData<Double>> parameterWithoutUnits = key.set(data1, data2);
        commonAssertions(keyName, JKeyType.DoubleArrayKey(), arrayData, parameterWithoutUnits, NoUnits);

        // key.set with Units
        Parameter<ArrayData<Double>> parameterWithUnits = keyUnits.setAll(arrayData);
        commonAssertions(keyName, JKeyType.DoubleArrayKey(), arrayData, parameterWithUnits, encoder);
    }
}
