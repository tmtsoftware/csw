package csw.params.core.models;

import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

// DEOPSCSW-183: Configure attributes and values
public class JArrayDataTest extends JUnitSuite {

    @Test
    public void shouldCreateArrayDataFromJavaArray__DEOPSCSW_183() {
        Byte[] byteData = {1, 2, 3};
        Short[] shortData = {10, 20, 30};
        Long[] longData = {100L, 200L, 300L};
        Integer[] intData = {1000, 2000, 3000};
        Float[] floatData = {10000.10f, 20000.20f, 30000.30f};
        Double[] doubleData = {100000.100d, 200000.200d, 300000.300d};

        ArrayData<Byte> byteArrayData = ArrayData.fromArray(byteData);
        ArrayData<Short> shortArrayData = ArrayData.fromArray(shortData);
        ArrayData<Long> longArrayData = ArrayData.fromArray(longData);
        ArrayData<Integer> integerArrayData = ArrayData.fromArray(intData);
        ArrayData<Float> floatArrayData = ArrayData.fromArray(floatData);
        ArrayData<Double> doubleArrayData = ArrayData.fromArray(doubleData);

        Byte[] actualByteValuesArray = (Byte[]) byteArrayData.values();
        Short[] actualShortValuesArray = (Short[]) shortArrayData.values();
        Long[] actualLongValuesArray = (Long[]) longArrayData.values();
        Integer[] actualIntValuesArray = (Integer[]) integerArrayData.values();
        Float[] actualFloatValuesArray = (Float[]) floatArrayData.values();
        Double[] actualDoubleValuesArray = (Double[]) doubleArrayData.values();

        Assert.assertArrayEquals(byteData, actualByteValuesArray);
        Assert.assertArrayEquals(shortData, actualShortValuesArray);
        Assert.assertArrayEquals(longData, actualLongValuesArray);
        Assert.assertArrayEquals(intData, actualIntValuesArray);
        Assert.assertArrayEquals(floatData, actualFloatValuesArray);
        Assert.assertArrayEquals(doubleData, actualDoubleValuesArray);
    }
}
