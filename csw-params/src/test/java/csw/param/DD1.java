package csw.param;

import csw_params.Events;
import csw_params.Keytype;
import csw_params.Parameter;
import csw_params.ParameterTypes;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DD1 {

    @Test
    public void booleanParam() {
        List<Boolean> booleans1 = Arrays.asList(true, false);
        ParameterTypes.BooleanItems booleanItems = ParameterTypes.
                BooleanItems
                .newBuilder()
                .addAllValues(booleans1)
                .addValues(true)
                .build();

        Parameter.PbParameter pbParameter = Parameter.PbParameter.newBuilder()
                .setBooleanItems(booleanItems)
                .build();
    }


    @Test
    public void stringArrayParam() {
        List<Integer> int1 = Arrays.asList(1, 2);
        List<Integer> int2 = Arrays.asList(3, 4);
        ParameterTypes.IntItems intItems1 = ParameterTypes.
                IntItems
                .newBuilder()
                .addAllValues(int1)
                .build();

        ParameterTypes.IntItems intItems2 = ParameterTypes.
                IntItems
                .newBuilder()
                .addAllValues(int2)
                .build();

        List<ParameterTypes.IntItems> intItems = Arrays.asList(intItems1, intItems2);

        ParameterTypes.IntArrayItems intArrayItems = ParameterTypes.IntArrayItems
                .newBuilder()
                .addAllValues(intItems)
                .build();

        Parameter.PbParameter pbParameter = Parameter.PbParameter.newBuilder()
                .setIntArrayItems(intArrayItems)
                .setKeyType(Keytype.PbKeyType.IntArrayKey)
                .build();

        System.out.println(pbParameter.getInstantItems());
        System.out.println(pbParameter.getIntArrayItems());
        System.out.println(pbParameter);
    }

    @Test
    public void event() {
        List<Integer> int1 = Arrays.asList(1, 2);
        List<Integer> int2 = Arrays.asList(3, 4);
        ParameterTypes.IntItems intItems1 = ParameterTypes.
                IntItems
                .newBuilder()
                .addAllValues(int1)
                .build();

        ParameterTypes.IntItems intItems2 = ParameterTypes.
                IntItems
                .newBuilder()
                .addAllValues(int2)
                .build();

        List<ParameterTypes.IntItems> intItems = Arrays.asList(intItems1, intItems2);

        ParameterTypes.IntArrayItems intArrayItems = ParameterTypes.IntArrayItems
                .newBuilder()
                .addAllValues(intItems)
                .build();

        Parameter.PbParameter pbParameter = Parameter.PbParameter.newBuilder()
                .setIntArrayItems(intArrayItems)
                .setKeyType(Keytype.PbKeyType.IntArrayKey)
                .build();


        Events.PbEvent pbEvent = Events.PbEvent.newBuilder()
                .setEventType(Events.PbEvent.PbEventType.StatusEvent)
                .addParamSet(pbParameter)
                .build();

        System.out.println(pbEvent);
    }
}
