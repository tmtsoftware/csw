package csw.param.generics;

import csw.param.models.RaDec;
import csw.param.models.Struct;
import org.junit.Assert;
import org.junit.Test;

import static csw.param.javadsl.JUnits.*;

// DEOPSCSW-190: Implement Unit Support
public class JSimpleKeyTypeTest {

    @Test
    public void testBooleanKeyParameter() {
        String keyName = "encoder";
        Key<Boolean> key = JKeyTypes.BooleanKey().make(keyName);
        Boolean[] paramData = {true, false, true};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.BooleanKey(),key.keyType());

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
        Key<Character> key = JKeyTypes.CharKey().make(keyName);
        Character[] paramData = {'a', 'b', 'c'};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.CharKey(),key.keyType());

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

    @Test
    public void testShortKeyParameter() {
        String keyName = "ShortKey";
        Key<Short> key = JKeyTypes.ShortKey().make(keyName);
        Short[] paramData = {10, 20, 30};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.ShortKey(),key.keyType());

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
        Key<Long> key = JKeyTypes.LongKey().make(keyName);
        Long[] paramData = {10L, 20L, 30L};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.LongKey(),key.keyType());

        // key.set without Units
        Parameter<Long> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Long[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Long> parameterWithUnits = key.set(paramData, micrometers);
        Assert.assertEquals(micrometers, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Long[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testIntegerKeyParameter() {
        String keyName = "IntegerKey";
        Key<Integer> key = JKeyTypes.IntKey().make(keyName);
        Integer[] paramData = {10, 20, 30};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.IntKey(),key.keyType());

        // key.set without Units
        Parameter<Integer> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Integer[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Integer> parameterWithUnits = key.set(paramData, milliseconds);
        Assert.assertEquals(milliseconds, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Integer[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testFloatKeyParameter() {
        String keyName = "FloatKey";
        Key<Float> key = JKeyTypes.FloatKey().make(keyName);
        Float[] paramData = {10.15f, 20.89f, 30f};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.FloatKey(),key.keyType());

        // key.set without Units
        Parameter<Float> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Float[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Float> parameterWithUnits = key.set(paramData, millimeters);
        Assert.assertEquals(millimeters, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Float[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testDoubleKeyParameter() {
        String keyName = "DoubleKey";
        Key<Double> key = JKeyTypes.DoubleKey().make(keyName);
        Double[] paramData = {10.89d, 20.25d, 30d};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.DoubleKey(),key.keyType());

        // key.set without Units
        Parameter<Double> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Double[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Double> parameterWithUnits = key.set(paramData, kilometers);
        Assert.assertEquals(kilometers, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Double[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testRaDecKeyParameter() {
        String keyName = "RaDecKey";
        Key<RaDec> key = JKeyTypes.RaDecKey().make(keyName);
        RaDec[] paramData = {RaDec.apply(10, 11.15), RaDec.apply(20.25, 21), RaDec.apply(30, 31)};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.RaDecKey(),key.keyType());

        // key.set without Units
        Parameter<RaDec> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (RaDec[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<RaDec> parameterWithUnits = key.set(paramData, meters);
        Assert.assertEquals(meters, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (RaDec[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }
    
    @Test
    public void testStringKeyParameter() {
        String keyName = "StringKey";
        Key<String> key = JKeyTypes.StringKey().make(keyName);
        String[] paramData = {"first", "seconds", "third"};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.StringKey(),key.keyType());

        // key.set without Units
        Parameter<String> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (String[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<String> parameterWithUnits = key.set(paramData, degrees);
        Assert.assertEquals(degrees, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (String[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }

    @Test
    public void testStructKeyParameter() {
        String keyName = "StructKey";
        Key<Struct> key = JKeyTypes.StructKey().make(keyName);

        Key<String> ra = JKeyTypes.StringKey().make("ra");
        Key<String> dec = JKeyTypes.StringKey().make("dec");
        Key<Double> epoch = JKeyTypes.DoubleKey().make("epoch");

        Struct struct1 = (Struct) new Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0));
        Struct struct2 = (Struct) new Struct().madd(ra.set("22:23:24.2"), dec.set("42:43:44.4"), epoch.set(2950.0));

        Struct[] paramData = {struct1, struct2};
        Assert.assertEquals(keyName, key.keyName());
        Assert.assertEquals(JKeyTypes.StructKey(),key.keyType());

        // key.set without Units
        Parameter<Struct> parameterWithoutUnits = key.set(paramData);

        Assert.assertArrayEquals(paramData, (Struct[])parameterWithoutUnits.values());

        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());

        // key.set with Units
        Parameter<Struct> parameterWithUnits = key.set(paramData, seconds);
        Assert.assertEquals(seconds, parameterWithUnits.units());

        Assert.assertArrayEquals(paramData, (Struct[])parameterWithUnits.values());
        Assert.assertEquals(paramData[0], parameterWithoutUnits.get(0).get());
        Assert.assertEquals(paramData[1], parameterWithoutUnits.value(1));
        Assert.assertEquals(paramData[0], parameterWithoutUnits.head());
        Assert.assertEquals(paramData.length, parameterWithoutUnits.size());
    }
    
}
