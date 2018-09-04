package csw.messages.params.models;

import csw.messages.params.generics.JKeyType;
import csw.messages.params.generics.Key;
import csw.messages.params.generics.Parameter;
import csw.messages.params.models.JStruct;
import csw.messages.params.models.Struct;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;

import static csw.messages.javadsl.JUnits.NoUnits;

// DEOPSCSW-183: Configure attributes and values
//DEOPSCSW-282: Add a timestamp Key and Parameter
public class JStructTest {

    private final String keyName = "StructKey";
    private final Key<Struct> key = JKeyType.StructKey().make(keyName);
    private final Key<String> ra = JKeyType.StringKey().make("ra");
    private final Key<String> dec = JKeyType.StringKey().make("dec");
    private final Key<Double> epoch = JKeyType.DoubleKey().make("epoch");
    private final Key<Instant> ts = JKeyType.TimestampKey().make("now");
    private final Parameter<String> raParameter = ra.set("12:13:14.1");
    private final Parameter<String> decParameter = dec.set("32:33:34.4");
    private final Parameter<Double> epochParameter = epoch.set(1950.0);
    private final Parameter<Instant> currentTimeParameter = ts.set(Instant.now());

    @Test
    public void shouldAbleToCreateWithVarArgs() {
        Struct struct1 = JStruct.create(raParameter, decParameter, epochParameter, currentTimeParameter);
        Struct struct2 = JStruct.create(decParameter, epochParameter);
        Parameter<Struct> structParameter = key.set(struct1, struct2);

        Assert.assertEquals(Arrays.asList(struct1, struct2), structParameter.jValues());
        Assert.assertArrayEquals(new Struct[]{struct1, struct2}, (Struct[])structParameter.values());
    }

    @Test
    public void shouldAbleToCreateWithSetOfParams() {

        HashSet<Parameter<?>> parameterHashSet1 = new HashSet<>(Arrays.asList(raParameter, decParameter, epochParameter));
        HashSet<Parameter<?>> parameterHashSet2 = new HashSet<>(Arrays.asList(decParameter, epochParameter, currentTimeParameter));
        Struct struct1 = JStruct.create(parameterHashSet1);
        Struct struct2 = JStruct.create(parameterHashSet2);

        Parameter<Struct> structParameter = key.set(new Struct[]{struct1, struct2}, NoUnits);

        Assert.assertEquals(Arrays.asList(struct1, struct2), structParameter.jValues());
        Assert.assertArrayEquals(new Struct[]{struct1, struct2}, (Struct[])structParameter.values());
    }
}
