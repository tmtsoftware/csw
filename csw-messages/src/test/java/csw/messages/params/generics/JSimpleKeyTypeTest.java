package csw.messages.params.generics;

import csw.messages.params.models.RaDec;
import csw.messages.params.models.Struct;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

import static csw.messages.javadsl.JUnits.*;

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
// DEOPSCSW-190: Implement Unit Support
// DEOPSCSW-184: Change configurations - attributes and values
public class JSimpleKeyTypeTest {

    @Test
    public void testBooleanKeyParameter() {
        String keyName = "encoder";
        Key<Boolean> key = JKeyType.BooleanKey().make(keyName);
        Boolean[] paramData = {true, false, true};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.BooleanKey(),key.keyType());

        // key.set without Units
        Parameter<Boolean> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Boolean[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Boolean> parameterWithUnits = key.set(paramData, encoder);
        Assert.assertEquals(encoder, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Boolean[])parameterWithoutUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testCharKeyParameter() {
        String keyName = "charKey";
        Key<Character> key = JKeyType.CharKey().make(keyName);
        Character[] paramData = {'a', 'b', 'c'};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.CharKey(),key.keyType());

        // key.set without Units
        Parameter<Character> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Character[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Character> parameterWithUnits = key.set(paramData, encoder);
        Assert.assertEquals(encoder, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Character[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    // DEOPSCSW-186: Binary value payload
    @Test
    public void testByteKeyParameter() {
        String keyName = "ByteKey";
        Key<Byte> key = JKeyType.ByteKey().make(keyName);
        Byte[] paramData = {-127, 100, 127};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.ByteKey(),key.keyType());

        // key.set without Units
        Parameter<Byte> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Byte[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Byte> parameterWithUnits = key.set(paramData, encoder);
        Assert.assertEquals(encoder, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Byte[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testShortKeyParameter() {
        String keyName = "ShortKey";
        Key<Short> key = JKeyType.ShortKey().make(keyName);
        Short[] paramData = {10, 20, 30};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.ShortKey(),key.keyType());

        // key.set without Units
        Parameter<Short> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Short[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Short> parameterWithUnits = key.set(paramData, encoder);
        Assert.assertEquals(encoder, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Short[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testLongKeyParameter() {
        String keyName = "LongKey";
        Key<Long> key = JKeyType.LongKey().make(keyName);
        Long[] paramData = {10L, 20L, 30L};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.LongKey(),key.keyType());

        // key.set without Units
        Parameter<Long> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Long[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Long> parameterWithUnits = key.set(paramData, micrometer);
        Assert.assertEquals(micrometer, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Long[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testIntegerKeyParameter() {
        String keyName = "IntegerKey";
        Key<Integer> key = JKeyType.IntKey().make(keyName);
        Integer[] paramData = {10, 20, 30};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.IntKey(),key.keyType());

        // key.set without Units
        Parameter<Integer> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Integer[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Integer> parameterWithUnits = key.set(paramData, millisecond);
        Assert.assertEquals(millisecond, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Integer[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testFloatKeyParameter() {
        String keyName = "FloatKey";
        Key<Float> key = JKeyType.FloatKey().make(keyName);
        Float[] paramData = {10.15f, 20.89f, 30f};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.FloatKey(),key.keyType());

        // key.set without Units
        Parameter<Float> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Float[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Float> parameterWithUnits = key.set(paramData, millimeter);
        Assert.assertEquals(millimeter, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Float[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testDoubleKeyParameter() {
        String keyName = "DoubleKey";
        Key<Double> key = JKeyType.DoubleKey().make(keyName);
        Double[] paramData = {10.89d, 20.25d, 30d};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.DoubleKey(),key.keyType());

        // key.set without Units
        Parameter<Double> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Double[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Double> parameterWithUnits = key.set(paramData, kilometer);
        Assert.assertEquals(kilometer, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Double[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    //DEOPSCSW-282: Add a timestamp Key and Parameter
    @Test
    public void testTimestampKeyParameter() {
        String keyName = "TimestampKey";
        Key<Instant> key = JKeyType.TimestampKey().make(keyName);
        Instant[] paramData = {Instant.now(), Instant.ofEpochSecond(3600)};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.TimestampKey(),key.keyType());

        // key.set without Units
        Parameter<Instant> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Instant[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Instant> parameterWithUnits = key.set(paramData, millisecond);
        Assert.assertEquals(millisecond, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Instant[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testRaDecKeyParameter() {
        String keyName = "RaDecKey";
        Key<RaDec> key = JKeyType.RaDecKey().make(keyName);
        RaDec[] paramData = {RaDec.apply(10, 11.15), RaDec.apply(20.25, 21), RaDec.apply(30, 31)};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.RaDecKey(),key.keyType());

        // key.set without Units
        Parameter<RaDec> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (RaDec[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<RaDec> parameterWithUnits = key.set(paramData, meter);
        Assert.assertEquals(meter, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (RaDec[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testStringKeyParameter() {
        String keyName = "StringKey";
        Key<String> key = JKeyType.StringKey().make(keyName);
        String[] paramData = {"first", "seconds", "third"};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.StringKey(),key.keyType());

        // key.set without Units
        Parameter<String> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (String[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<String> parameterWithUnits = key.set(paramData, degree);
        Assert.assertEquals(degree, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (String[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testStructKeyParameter() {
        String keyName = "StructKey";
        Key<Struct> key = JKeyType.StructKey().make(keyName);

        Key<String> ra = JKeyType.StringKey().make("ra");
        Key<String> dec = JKeyType.StringKey().make("dec");
        Key<Double> epoch = JKeyType.DoubleKey().make("epoch");

        Struct struct1 = (Struct) new Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0));
        Struct struct2 = (Struct) new Struct().madd(ra.set("22:23:24.2"), dec.set("42:43:44.4"), epoch.set(2950.0));

        Struct[] paramData = {struct1, struct2};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyType.StructKey(),key.keyType());

        // key.set without Units
        Parameter<Struct> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Struct[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Struct> parameterWithUnits = key.set(paramData, second);
        Assert.assertEquals(second, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Struct[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

}
