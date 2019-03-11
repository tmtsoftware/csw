package csw.params.core.models;

import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.javadsl.JKeyType;
import csw.time.core.models.TAITime;
import csw.time.core.models.UTCTime;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.Arrays;
import java.util.Set;

import static csw.params.javadsl.JUnits.NoUnits;

// DEOPSCSW-183: Configure attributes and values
//DEOPSCSW-282: Add a timestamp Key and Parameter
public class JStructTest extends JUnitSuite {

    private final String keyName = "StructKey";
    private final Key<Struct> key = JKeyType.StructKey().make(keyName);
    private final Key<String> ra = JKeyType.StringKey().make("ra");
    private final Key<String> dec = JKeyType.StringKey().make("dec");
    private final Key<Double> epoch = JKeyType.DoubleKey().make("epoch");
    private final Key<UTCTime> utcTime = JKeyType.UTCTimeKey().make("now");
    private final Key<TAITime> taiTime = JKeyType.TAITimeKey().make("tai_now");
    private final Parameter<String> raParameter = ra.set("12:13:14.1");
    private final Parameter<String> decParameter = dec.set("32:33:34.4");
    private final Parameter<Double> epochParameter = epoch.set(1950.0);
    private final Parameter<UTCTime> currentTimeParameter = utcTime.set(UTCTime.now());
    private final Parameter<TAITime> taiTimeParameter = taiTime.set(TAITime.now());

    @Test
    public void shouldAbleToCreateWithVarArgs() {
        Struct struct1 = JStruct.create(raParameter, decParameter, epochParameter, currentTimeParameter, taiTimeParameter);
        Struct struct2 = JStruct.create(decParameter, epochParameter);
        Parameter<Struct> structParameter = key.set(struct1, struct2);

        Assert.assertEquals(Arrays.asList(struct1, struct2), structParameter.jValues());
        Assert.assertArrayEquals(new Struct[]{struct1, struct2}, (Struct[])structParameter.values());
    }

    @Test
    public void shouldAbleToCreateWithSetOfParams() {

        Set<Parameter<?>> parameterSet1 = Set.of(raParameter, decParameter, epochParameter);
        Set<Parameter<?>> parameterSet2 = Set.of(decParameter, epochParameter, currentTimeParameter, taiTimeParameter);
        Struct struct1 = JStruct.create(parameterSet1);
        Struct struct2 = JStruct.create(parameterSet2);

        Parameter<Struct> structParameter = key.set(new Struct[]{struct1, struct2}, NoUnits);

        Assert.assertEquals(Arrays.asList(struct1, struct2), structParameter.jValues());
        Assert.assertArrayEquals(new Struct[]{struct1, struct2}, (Struct[])structParameter.values());
    }
}
