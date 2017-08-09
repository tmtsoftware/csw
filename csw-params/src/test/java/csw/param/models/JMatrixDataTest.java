package csw.param.models;

import org.junit.Assert;
import org.junit.Test;

// DEOPSCSW-183: Configure attributes and values
public class JMatrixDataTest {

    @Test
    public void shouldCreateMatrixDataFromJavaArrays() {
        Byte[][] byteData = {{1, 2, 3}, {4, 5, 6}};
        Short[][] shortData = {{10, 20, 30}, {40, 50, 60}};
        Long[][] longData = {{100L, 200L, 300L}, {400L, 500L, 600L}};
        Integer[][] intData = {{1000, 2000, 3000}, {4000, 5000, 6000}};
        Float[][] floatData = {{10000.10f, 20000.20f, 30000.30f}, {40000.40f, 50000f, 60000f}};
        Double[][] doubleData = {{100000.100d, 200000.200d, 300000.300d}, {400000.400d, 500000d, 600000d}};


        MatrixData<Byte> byteMatrixData = JMatrixData.fromArrays(Byte.class, byteData);
        MatrixData<Short> shortMatrixData = JMatrixData.fromArrays(Short.class, shortData);
        MatrixData<Long> longMatrixData = JMatrixData.fromArrays(Long.class, longData);
        MatrixData<Integer> integerMatrixData = JMatrixData.fromArrays(Integer.class, intData);
        MatrixData<Float> floatMatrixData = JMatrixData.fromArrays(Float.class, floatData);
        MatrixData<Double> doubleMatrixData = JMatrixData.fromArrays(Double.class, doubleData);

        Byte[][] actualByteValuesArray = (Byte[][])byteMatrixData.values();
        Short[][] actualShortValuesArray = (Short[][])shortMatrixData.values();
        Long[][] actualLongValuesArray = (Long[][])longMatrixData.values();
        Integer[][] actualIntValuesArray = (Integer[][])integerMatrixData.values();
        Float[][] actualFloatValuesArray = (Float[][])floatMatrixData.values();
        Double[][] actualDoubleValuesArray = (Double[][])doubleMatrixData.values();


        Assert.assertArrayEquals(byteData, actualByteValuesArray);
        Assert.assertArrayEquals(shortData, actualShortValuesArray);
        Assert.assertArrayEquals(longData, actualLongValuesArray);
        Assert.assertArrayEquals(intData, actualIntValuesArray);
        Assert.assertArrayEquals(floatData, actualFloatValuesArray);
        Assert.assertArrayEquals(doubleData, actualDoubleValuesArray);
    }


}
