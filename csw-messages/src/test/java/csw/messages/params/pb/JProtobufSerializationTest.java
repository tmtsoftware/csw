package csw.messages.params.pb;

import csw.messages.events.Event;
import csw.messages.events.Event$;
import csw.messages.events.EventName;
import csw.messages.events.SystemEvent;
import csw.messages.params.generics.JKeyTypes;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.*;
import csw_protobuf.events.PbEvent;
import csw_protobuf.parameter.PbParameter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

// DEOPSCSW-495: Protobuf serde fails for Java keys/parameters
@RunWith(value = Parameterized.class)
public class JProtobufSerializationTest {

    private final String keyType;
    private final Parameter<?> param;

    public JProtobufSerializationTest(String keyType, Parameter<?> param) {
        this.keyType = keyType;
        this.param = param;
    }

    @Parameterized.Parameters(name = "{index}: KeyType={0}")
    public static List<Object[]> data() {
        Byte[] byteData = {1, 2, 3};
        Short[] shortData = {10, 20, 30};
        Long[] longData = {100L, 200L, 300L};
        Integer[] intData = {1000, 2000, 3000};
        Float[] floatData = {10000.10f, 20000.20f, 30000.30f};
        Double[] doubleData = {100000.100d, 200000.200d, 300000.300d};

        Byte[][] byteData2D = {{1, 2, 3}, {4, 5, 6}};
        Short[][] shortData2D = {{10, 20, 30}, {40, 50, 60}};
        Long[][] longData2D = {{100L, 200L, 300L}, {400L, 500L, 600L}};
        Integer[][] intData2D = {{1000, 2000, 3000}, {4000, 5000, 6000}};
        Float[][] floatData2D = {{10000.10f, 20000.20f, 30000.30f}, {40000.40f, 50000f, 60000f}};
        Double[][] doubleData2D = {{100000.100d, 200000.200d, 300000.300d}, {400000.400d, 500000d, 600000d}};

        MatrixData<Byte> byteMatrixData = MatrixData.fromJavaArrays(Byte.class, byteData2D);
        MatrixData<Short> shortMatrixData = MatrixData.fromJavaArrays(Short.class, shortData2D);
        MatrixData<Long> longMatrixData = MatrixData.fromJavaArrays(Long.class, longData2D);
        MatrixData<Integer> integerMatrixData = MatrixData.fromJavaArrays(Integer.class, intData2D);
        MatrixData<Float> floatMatrixData = MatrixData.fromJavaArrays(Float.class, floatData2D);
        MatrixData<Double> doubleMatrixData = MatrixData.fromJavaArrays(Double.class, doubleData2D);

        return Arrays.asList(new Object[][]{
                {"JChoiceKey", JKeyTypes.ChoiceKey().make("choiceKey", Choices.from("A", "B", "C")).set(new Choice("A"))},
                {"JRaDecKey", JKeyTypes.RaDecKey().make("raDecKey").set(new RaDec(10, 20))},
                {"JStructKey", JKeyTypes.StructKey().make("structKey").set(new Struct().add(JKeyTypes.RaDecKey().make("raDecKey").set(new RaDec(10, 20))))},
                {"JStringKey", JKeyTypes.StringKey().make("stringKey").set("str1", "str2")},
                {"JTimestampKey", JKeyTypes.TimestampKey().make("timestampKey").set(Instant.now())},
                {"JBooleanKey", JKeyTypes.BooleanKey().make("booleanKey").set(true)},
                {"JCharKey", JKeyTypes.CharKey().make("charKey").set('A', 'B')},
                {"JByteKey", JKeyTypes.ByteKey().make("byteKey").set(byteData)},
                {"JShortKey", JKeyTypes.ShortKey().make("shortKey").set(shortData)},
                {"JLongKey", JKeyTypes.LongKey().make("longKey").set(longData)},
                {"JIntKey", JKeyTypes.IntKey().make("intKey").set(intData)},
                {"JFloatKey", JKeyTypes.FloatKey().make("floatKey").set(floatData)},
                {"JDoubleKey", JKeyTypes.DoubleKey().make("doubleKey").set(doubleData)},
                {"JByteArrayKey", JKeyTypes.ByteArrayKey().make("byteArrayKey").set(ArrayData.fromJavaArray(byteData))},
                {"JShortArrayKey", JKeyTypes.ShortArrayKey().make("shortArrayKey").set(ArrayData.fromJavaArray(shortData))},
                {"JLongArrayKey", JKeyTypes.LongArrayKey().make("longArrayKey").set(ArrayData.fromJavaArray(longData))},
                {"JIntArrayKey", JKeyTypes.IntArrayKey().make("intArrayKey").set(ArrayData.fromJavaArray(intData))},
                {"JFloatArrayKey", JKeyTypes.FloatArrayKey().make("floatArrayKey").set(ArrayData.fromJavaArray(floatData))},
                {"JDoubleArrayKey", JKeyTypes.DoubleArrayKey().make("doubleArrayKey").set(ArrayData.fromJavaArray(doubleData))},
                {"JByteMatrixKey", JKeyTypes.ByteMatrixKey().make("byteMatrixKey").set(byteMatrixData)},
                {"JShortMatrixKey", JKeyTypes.ShortMatrixKey().make("shortMatrixKey").set(shortMatrixData)},
                {"JLongMatrixKey", JKeyTypes.LongMatrixKey().make("longMatrixKey").set(longMatrixData)},
                {"JIntMatrixKey", JKeyTypes.IntMatrixKey().make("intMatrixKey").set(integerMatrixData)},
                {"JFloatMatrixKey", JKeyTypes.FloatMatrixKey().make("floatMatrixKey").set(floatMatrixData)},
                {"JDoubleMatrixKey", JKeyTypes.DoubleMatrixKey().make("doubleMatrixKey").set(doubleMatrixData)}
        });
    }

    @Test
    public void shouldAbleToConvertToAndFromPbParameterAndEvent() {
        // ===== Test PbParameter SERDE =====
        PbParameter pbParameter = Parameter.typeMapper2().toBase(param);
        byte[] pbParameterBytes = pbParameter.toByteArray();
        PbParameter pbParameterFromBytes = (PbParameter) PbParameter.parseFrom(pbParameterBytes);

        Assert.assertEquals(pbParameter, pbParameterFromBytes);

        // ===== Test Event SERDE =====
        Prefix source = new Prefix("wfos.filter");
        EventName eventName = new EventName("move");
        SystemEvent originalEvent = new SystemEvent(source, eventName).add(param);

        PbEvent pbEvent = originalEvent.toPb();
        byte[] pbEventBytes = pbEvent.toByteArray();
        PbEvent pbEventFromBytes = (PbEvent) PbEvent.parseFrom(pbEventBytes);
        Assert.assertEquals(pbEventFromBytes, pbEvent);

        Event eventFromPbEvent = Event$.MODULE$.fromPb(pbEventFromBytes);
        Assert.assertEquals(eventFromPbEvent, originalEvent);
    }
}
