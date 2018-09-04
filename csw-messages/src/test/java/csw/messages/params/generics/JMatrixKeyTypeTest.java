package csw.messages.params.generics;

import csw.messages.params.models.MatrixData;
import csw.messages.params.models.Units;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static csw.messages.javadsl.JUnits.*;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-190: Implement Unit Support
// DEOPSCSW-184: Change configurations - attributes and values
@RunWith(value = Parameterized.class)
public class JMatrixKeyTypeTest {

    private final String keyName;
    private final SimpleKeyType<MatrixData> matrixKey;
    private final Object[][] data;
    private final Optional<Units> units;

    public JMatrixKeyTypeTest(String keyName, SimpleKeyType<MatrixData> keyType, Object[][] data, Optional<Units> units) {
        this.keyName = keyName;
        this.matrixKey = keyType;
        this.data = data;
        this.units = units;
    }

    @Parameterized.Parameters(name = "{index}: KeyType={1}, units={3}")
    public static Iterable<Object[]> data() {
        Byte[][] byteData = {{1, 2, 3}, {4, 5, 6}};
        Short[][] shortData = {{10, 20, 30}, {40, 50, 60}};
        Long[][] longData = {{100L, 200L, 300L}, {400L, 500L, 600L}};
        Integer[][] intData = {{1000, 2000, 3000}, {4000, 5000, 6000}};
        Float[][] floatData = {{10000.10f, 20000.20f, 30000.30f}, {40000.40f, 50000f, 60000f}};
        Double[][] doubleData = {{100000.100d, 200000.200d, 300000.300d}, {400000.400d, 500000d, 600000d}};

        return Arrays.asList(new Object[][]{
                        {"byteKey1", JKeyType.ByteMatrixKey(), byteData, Optional.empty()},
                        {"byteKey2", JKeyType.ByteMatrixKey(), byteData, Optional.of(encoder)},
                        {"shortKey1", JKeyType.ShortMatrixKey(), shortData, Optional.empty()},
                        {"shortKey2", JKeyType.ShortMatrixKey(), shortData, Optional.of(degree)},
                        {"longKey1", JKeyType.LongMatrixKey(), longData, Optional.empty()},
                        {"longKey2", JKeyType.LongMatrixKey(), longData, Optional.of(kilometer)},
                        {"intKey1", JKeyType.IntMatrixKey(), intData, Optional.empty()},
                        {"intKey2", JKeyType.IntMatrixKey(), intData, Optional.of(meter)},
                        {"floatKey1", JKeyType.FloatMatrixKey(), floatData, Optional.empty()},
                        {"floatKey2", JKeyType.FloatMatrixKey(), floatData, Optional.of(millimeter)},
                        {"doubleKey1", JKeyType.DoubleMatrixKey(), doubleData, Optional.empty()},
                        {"doubleKey2", JKeyType.DoubleMatrixKey(), doubleData, Optional.of(millisecond)}
                }
        );
    }

    @Test
    public void matrixKeyParameterTest() {
        Parameter parameter;
        MatrixData matrixData = MatrixData.fromJavaArrays(Object.class, data);
        MatrixData[] paramValues = {matrixData};

        if (units.isPresent()) parameter = matrixKey.make(keyName).set(paramValues, units.get());
        else parameter = matrixKey.make(keyName).set(paramValues);

        Assert.assertEquals(keyName, parameter.keyName());
        Assert.assertEquals(units.orElse(NoUnits), parameter.units());
        Assert.assertEquals(paramValues.length, parameter.size());
        Assert.assertEquals(matrixData, parameter.head());
        Assert.assertEquals(matrixData, parameter.get(0).get());
        Assert.assertEquals(matrixData, parameter.value(0));
        Assert.assertEquals(paramValues, parameter.values());
        Assert.assertEquals(Collections.singletonList(matrixData), parameter.jValues());

    }
}
