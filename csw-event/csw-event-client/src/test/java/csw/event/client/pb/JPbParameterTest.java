package csw.event.client.pb;

import csw_protobuf.Events;
import csw_protobuf.Keytype;
import csw_protobuf.Parameter;
import csw_protobuf.ParameterTypes;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.Arrays;
import java.util.List;

//FIXME: need asserts in all test
public class JPbParameterTest extends JUnitSuite {

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

//        System.out.println(pbParameter.getInstantItems());
//        System.out.println(pbParameter.getIntArrayItems());
//        System.out.println(pbParameter);
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
                .setEventType(Events.PbEvent.PbEventType.SystemEvent)
                .addParamSet(pbParameter)
                .build();

//        System.out.println(pbEvent);
    }
}
