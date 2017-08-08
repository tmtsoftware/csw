package csw.param.models;

import csw.param.generics.JKeyTypes;
import csw.param.generics.Key;
import csw.param.generics.Parameter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static csw.param.javadsl.JUnits.NoUnits;

public class JStructTest {

    private final String keyName = "StructKey";
    private final Key<Struct> key = JKeyTypes.StructKey().make(keyName);
    private final Key<String> ra = JKeyTypes.StringKey().make("ra");
    private final Key<String> dec = JKeyTypes.StringKey().make("dec");
    private final Key<Double> epoch = JKeyTypes.DoubleKey().make("epoch");
    private final Parameter<String> raParameter = ra.set("12:13:14.1");
    private final Parameter<String> decParameter = dec.set("32:33:34.4");
    private final Parameter<Double> epochParameter = epoch.set(1950.0);

    @Test
    public void shouldAbleToCreateWithVarArgs() {
        Struct struct1 = JStruct.create(raParameter, decParameter, epochParameter);
        Struct struct2 = JStruct.create(decParameter, epochParameter);
        Parameter<Struct> structParameter = key.set(struct1, struct2);

        Assert.assertEquals(Arrays.asList(struct1, struct2), structParameter.jValues());
        Assert.assertArrayEquals(new Struct[]{struct1, struct2}, (Struct[])structParameter.values());
    }

    @Test
    public void shouldAbleToCreateWithSetOfParams() {

        HashSet<Parameter<?>> parameterHashSet1 = new HashSet<>(Arrays.asList(raParameter, decParameter, epochParameter));
        HashSet<Parameter<?>> parameterHashSet2 = new HashSet<>(Arrays.asList(decParameter, epochParameter));
        Struct struct1 = JStruct.create(parameterHashSet1);
        Struct struct2 = JStruct.create(parameterHashSet2);

        Parameter<Struct> structParameter = key.set(new Struct[]{struct1, struct2}, NoUnits);

        Assert.assertEquals(Arrays.asList(struct1, struct2), structParameter.jValues());
        Assert.assertArrayEquals(new Struct[]{struct1, struct2}, (Struct[])structParameter.values());
    }
}
