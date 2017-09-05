package csw.param.models;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

// DEOPSCSW-183: Configure attributes and values
//DEOPSCSW-282: Add a timestamp Key and Parameter
public class JArrayDataTest {

    @Test
    public void shouldCreateArrayDataFromJavaArray() {
        Byte[] byteData = {1, 2, 3};
        Short[] shortData = {10, 20, 30};
        Long[] longData = {100L, 200L, 300L};
        Integer[] intData = {1000, 2000, 3000};
        Float[] floatData = {10000.10f, 20000.20f, 30000.30f};
        Double[] doubleData = {100000.100d, 200000.200d, 300000.300d};
        Instant[] timestampData = {Instant.now(), Instant.ofEpochSecond(3600)};

        ArrayData<Byte> byteArrayData = JArrayData.fromArray(byteData);
        ArrayData<Short> shortArrayData = JArrayData.fromArray(shortData);
        ArrayData<Long> longArrayData = JArrayData.fromArray(longData);
        ArrayData<Integer> integerArrayData = JArrayData.fromArray(intData);
        ArrayData<Float> floatArrayData = JArrayData.fromArray(floatData);
        ArrayData<Double> doubleArrayData = JArrayData.fromArray(doubleData);
        ArrayData<Instant> timestampArrayData = JArrayData.fromArray(timestampData);

        Byte[] actualByteValuesArray = (Byte[])byteArrayData.values();
        Short[] actualShortValuesArray = (Short[])shortArrayData.values();
        Long[] actualLongValuesArray = (Long[])longArrayData.values();
        Integer[] actualIntValuesArray = (Integer[])integerArrayData.values();
        Float[] actualFloatValuesArray = (Float[])floatArrayData.values();
        Double[] actualDoubleValuesArray = (Double[])doubleArrayData.values();
        Instant[] actualTimestampValuesArray = (Instant[])timestampArrayData.values();


        Assert.assertArrayEquals(byteData, actualByteValuesArray);
        Assert.assertArrayEquals(shortData, actualShortValuesArray);
        Assert.assertArrayEquals(longData, actualLongValuesArray);
        Assert.assertArrayEquals(intData, actualIntValuesArray);
        Assert.assertArrayEquals(floatData, actualFloatValuesArray);
        Assert.assertArrayEquals(doubleData, actualDoubleValuesArray);
        Assert.assertArrayEquals(timestampData, actualTimestampValuesArray);
    }


}
