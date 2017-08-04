package csw.param.models;

import csw.param.parameters.JKeyTypes;
import csw.param.parameters.Key;
import csw.param.parameters.Parameter;
import csw.units.Units;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class JStructTest {

    String keyName = "StructKey";
    Key<Struct> key = JKeyTypes.StructKey().make(keyName);
    Key<String> ra = JKeyTypes.StringKey().make("ra");
    Key<String> dec = JKeyTypes.StringKey().make("dec");
    Key<Double> epoch = JKeyTypes.DoubleKey().make("epoch");
    Parameter<String> raParameter = ra.set("12:13:14.1");
    Parameter<String> decParameter = dec.set("32:33:34.4");
    Parameter<Double> epochParameter = epoch.set(1950.0);

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

        Parameter<Struct> structParameter = key.set(new Struct[]{struct1, struct2}, Units.NoUnits$.MODULE$);

        Assert.assertEquals(Arrays.asList(struct1, struct2), structParameter.jValues());
        Assert.assertArrayEquals(new Struct[]{struct1, struct2}, (Struct[])structParameter.values());
    }
}
