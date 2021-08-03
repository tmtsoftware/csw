package csw.params.core.generics;

import csw.params.core.models.Struct;
import csw.params.javadsl.JKeyType;
import csw.time.core.models.TAITime;
import csw.time.core.models.UTCTime;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.time.Instant;

import static csw.params.javadsl.JUnits.*;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
// DEOPSCSW-190: Implement Unit Support
// DEOPSCSW-184: Change configurations - attributes and values
@SuppressWarnings({"RedundantCast", "unchecked"}) // false negative, test fails if you remove explicit type.
public class JSimpleKeyTypeTest extends JUnitSuite {

    @Test
    public void testBooleanKeyParameter__DEOPSCSW_183_DEOPSCSW_185_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "encoder()";
        Key<Boolean> key = JKeyType.BooleanKey().make(keyName);
        Boolean[] paramData = {true, false, true};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.BooleanKey(), key.keyType());

        // key.set without Units
        Parameter<Boolean> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (Boolean[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testCharKeyParameter__DEOPSCSW_183_DEOPSCSW_185_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "charKey";
        Key<Character> key = JKeyType.CharKey().make(keyName, NoUnits);
        Key<Character> keyUnits = JKeyType.CharKey().make(keyName, encoder);
        Character[] paramData = {'a', 'b', 'c'};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.CharKey(), key.keyType());

        // key.set without Units
        Parameter<Character> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (Character[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Character> parameterWithUnits = keyUnits.setAll(paramData);
        Assert.assertEquals(encoder, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Character[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    // DEOPSCSW-186: Binary value payload
    @Test
    public void testByteKeyParameter__DEOPSCSW_185_DEOPSCSW_183_DEOPSCSW_190_DEOPSCSW_184_DEOPSCSW_186() {
        String keyName = "ByteKey";
        Key<Byte> key = JKeyType.ByteKey().make(keyName, NoUnits);
        Key<Byte> keyUnits = JKeyType.ByteKey().make(keyName, encoder);
        Byte[] paramData = {-127, 100, 127};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.ByteKey(), key.keyType());

        // key.set without Units
        Parameter<Byte> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (Byte[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Byte> parameterWithUnits = keyUnits.setAll(paramData);
        Assert.assertEquals(encoder, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Byte[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

    @Test
    public void testShortKeyParameter__DEOPSCSW_183_DEOPSCSW_185_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "ShortKey";
        Key<Short> key = JKeyType.ShortKey().make(keyName, NoUnits);
        Key<Short> keyUnits = JKeyType.ShortKey().make(keyName, encoder);
        Short[] paramData = {10, 20, 30};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.ShortKey(), key.keyType());

        // key.set without Units
        Parameter<Short> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (Short[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Short> parameterWithUnits = keyUnits.setAll(paramData);
        Assert.assertEquals(encoder, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Short[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

    @Test
    public void testLongKeyParameter__DEOPSCSW_183_DEOPSCSW_185_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "LongKey";
        Key<Long> key = JKeyType.LongKey().make(keyName, NoUnits);
        Key<Long> keyUnits = JKeyType.LongKey().make(keyName, micrometer);
        Long[] paramData = {10L, 20L, 30L};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.LongKey(), key.keyType());

        // key.set without Units
        Parameter<Long> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (Long[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Long> parameterWithUnits = keyUnits.setAll(paramData);
        Assert.assertEquals(micrometer, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Long[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

    @Test
    public void testIntegerKeyParameter__DEOPSCSW_183_DEOPSCSW_185_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "IntegerKey";
        Key<Integer> key = JKeyType.IntKey().make(keyName, NoUnits);
        Key<Integer> keyUnits = JKeyType.IntKey().make(keyName, millisecond);
        Integer[] paramData = {10, 20, 30};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.IntKey(), key.keyType());

        // key.set without Units
        Parameter<Integer> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (Integer[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Integer> parameterWithUnits = keyUnits.setAll(paramData);
        Assert.assertEquals(millisecond, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Integer[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

    @Test
    public void testFloatKeyParameter__DEOPSCSW_183_DEOPSCSW_185_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "FloatKey";
        Key<Float> key = JKeyType.FloatKey().make(keyName, NoUnits);
        Key<Float> keyUnits = JKeyType.FloatKey().make(keyName, millimeter);
        Float[] paramData = {10.15f, 20.89f, 30f};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.FloatKey(), key.keyType());

        // key.set without Units
        Parameter<Float> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (Float[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Float> parameterWithUnits = keyUnits.setAll(paramData);
        Assert.assertEquals(millimeter, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Float[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

    @Test
    public void testDoubleKeyParameter__DEOPSCSW_183_DEOPSCSW_185_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "DoubleKey";
        Key<Double> key = JKeyType.DoubleKey().make(keyName, NoUnits);
        Key<Double> keyUnits = JKeyType.DoubleKey().make(keyName, kilometer);
        Double[] paramData = {10.89d, 20.25d, 30d};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.DoubleKey(), key.keyType());

        // key.set without Units
        Parameter<Double> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (Double[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Double> parameterWithUnits = keyUnits.setAll(paramData);
        Assert.assertEquals(kilometer, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Double[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

    //DEOPSCSW-282: Add a timestamp Key and Parameter
    //DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
    @Test
    public void testUTCTimeKeyParameter__DEOPSCSW_661_DEOPSCSW_185_DEOPSCSW_282_DEOPSCSW_183_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "UTCTimeKey";
        Key<UTCTime> key = JKeyType.UTCTimeKey().make(keyName);
        UTCTime[] paramData = {UTCTime.now(), new UTCTime(Instant.ofEpochSecond(3600))};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.UTCTimeKey(), key.keyType());

        // key.set without Units
        Parameter<UTCTime> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (UTCTime[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<UTCTime> parameterWithUnits = key.setAll(paramData).withUnits(millisecond);
        Assert.assertEquals(millisecond, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (UTCTime[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

    //DEOPSCSW-661: Create UTCTimeKey and TAITimeKey replacing TimestampKey in Protobuf parameters
    @Test
    public void testTAITimeKeyParameter__DEOPSCSW_661_DEOPSCSW_185_DEOPSCSW_183_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "TAITimeKey";
        Key<TAITime> key = JKeyType.TAITimeKey().make(keyName);
        TAITime[] paramData = {TAITime.now(), new TAITime(Instant.ofEpochSecond(3600))};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.TAITimeKey(), key.keyType());

        // key.set without Units
        Parameter<TAITime> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (TAITime[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<TAITime> parameterWithUnits = key.setAll(paramData).withUnits(millisecond);
        Assert.assertEquals(millisecond, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (TAITime[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

    @Test
    public void testStringKeyParameter__DEOPSCSW_183_DEOPSCSW_185_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "StringKey";
        Key<String> key = JKeyType.StringKey().make(keyName, NoUnits);
        Key<String> keyUnits = JKeyType.StringKey().make(keyName, degree);
        String[] paramData = {"first", "seconds", "third"};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.StringKey(), key.keyType());

        // key.set without Units
        Parameter<String> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (String[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<String> parameterWithUnits = keyUnits.setAll(paramData);
        Assert.assertEquals(degree, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (String[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

    @Test
    public void testStructKeyParameter__DEOPSCSW_183_DEOPSCSW_185_DEOPSCSW_190_DEOPSCSW_184() {
        String keyName = "StructKey";
        Key<Struct> key = JKeyType.StructKey().make(keyName, NoUnits);

        Key<String> ra = JKeyType.StringKey().make("ra", NoUnits);
        Key<String> dec = JKeyType.StringKey().make("dec", NoUnits);
        Key<Double> epoch = JKeyType.DoubleKey().make("epoch", year);

        Struct struct1 = (Struct) new Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0));
        Struct struct2 = (Struct) new Struct().madd(ra.set("22:23:24.2"), dec.set("42:43:44.4"), epoch.set(2950.0));

        Struct[] paramData = {struct1, struct2};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.StructKey(), key.keyType());

        // key.set without Units
        Parameter<Struct> parameterWithoutUnits = key.setAll(paramData);

        Assert.assertArrayEquals(paramData, (Struct[]) parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Struct> parameterWithUnits = key.setAll(paramData);
        Assert.assertEquals(NoUnits, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Struct[]) parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithUnits.head());
        Assert.assertEquals(paramData.length, parameterWithUnits.size());
    }

}
