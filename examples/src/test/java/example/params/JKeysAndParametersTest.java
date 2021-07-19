package example.params;

import csw.params.core.generics.GChoiceKey;
import csw.params.core.generics.Key;
import csw.params.core.generics.Parameter;
import csw.params.core.models.*;
import csw.params.core.models.Coords.*;
import csw.params.javadsl.JKeyType;
import csw.params.javadsl.JUnits;
import csw.time.core.models.TAITime;
import csw.time.core.models.UTCTime;
import org.junit.Assert;
import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;

import java.util.*;

import static csw.params.core.models.Coords.*;
import static csw.params.core.models.JCoords.*;

@SuppressWarnings({"unused", "RedundantCast", "unchecked", "ArraysAsListWithZeroOrOneArgument"})
public class JKeysAndParametersTest extends JUnitSuite {

    @Test
    public void showUsageOfPrimitiveTypes() {
        //#primitives
        //making 3 keys
        String keyName = "encoder";
        Key<Boolean> k1 = JKeyType.BooleanKey().make(keyName);
        Key<Short> k2 = JKeyType.ShortKey().make(keyName, JUnits.NoUnits);
        Key<String> k3 = JKeyType.StringKey().make(keyName, JUnits.day);

        //storing a single value
        Parameter<Boolean> booleanParam = k1.set(true);

        //storing multiple values
        Short[] shortArray = {1, 2, 3, 4};
        Parameter<Short> paramWithManyShorts1 = k2.setAll(shortArray);
        Parameter<Short> paramWithManyShorts2 = k2.set((short) 1, (short) 2, (short) 3, (short) 4);

        //associating units
        String[] weekDays = {"Sunday", "Monday", "Tuesday"};
        Parameter<String> paramWithUnits1 = k3.setAll(weekDays);
        Parameter<String> paramWithUnits2 = k3.setAll(weekDays).withUnits(JUnits.day);

        //deault unit is NoUnits()
        boolean hasDefaultUnit = booleanParam.units() == JUnits.NoUnits; //true

        //set units explicitly on an existing Parameter
        Parameter<Short> paramWithUnits3 = paramWithManyShorts1.withUnits(JUnits.meter);

        //retrieve values from Parameter
        Short[] allValues = (Short[]) paramWithManyShorts1.values();

        //retrieve just top value
        Short head = paramWithManyShorts1.head();
        //#primitives

        //validations
        Assert.assertArrayEquals(shortArray, allValues);
        Assert.assertArrayEquals(shortArray, (Short[]) paramWithManyShorts1.values());
        Assert.assertArrayEquals(shortArray, (Short[]) paramWithManyShorts2.values());
        Assert.assertArrayEquals(weekDays, (String[]) paramWithUnits1.values());
        Assert.assertArrayEquals(weekDays, (String[]) paramWithUnits2.values());
        Assert.assertEquals(JUnits.day, paramWithUnits1.units());
        Assert.assertEquals(JUnits.day, paramWithUnits2.units());
        Assert.assertEquals(JUnits.meter, paramWithUnits3.units());
        Assert.assertEquals(1, (short) head);
    }

    @Test
    public void showUsageOfArrays() {
        //#arrays
        //make some arrays
        Double[] arr1 = {1.0, 2.0, 3.0, 4.0, 5.0};
        Double[] arr2 = {10.0, 20.0, 30.0, 40.0, 50.0};

        //keys
        Key<ArrayData<Double>> filterKey = JKeyType.DoubleArrayKey().make("filter", JUnits.NoUnits);

        //Store some values using helper method in ArrayData
        Parameter<ArrayData<Double>> p1 = filterKey.set(ArrayData.fromArray(arr1), ArrayData.fromArray(arr2));
        Parameter<ArrayData<Double>> p2 = filterKey.set(ArrayData.fromArray(arr2)).withUnits(JUnits.liter);

        //add units to existing parameters
        Parameter<ArrayData<Double>> p1AsCount = p1.withUnits(JUnits.count);

        //default unit is NoUnits()
        boolean bDefaultUnit = JUnits.NoUnits == p1.units();

        //retrieving values
        List<Double> head = p1.head().jValues();
        List<ArrayData<Double>> listOfArrayData = p1.jValues();
        Double[] arrayOfDoubles = (Double[]) p2.jValues().get(0).values();
        //#arrays

        //validations
        Assert.assertEquals(JUnits.NoUnits, p1.units());
        Assert.assertEquals(JUnits.liter, p2.units());
        Assert.assertEquals(JUnits.count, p1AsCount.units());
        Assert.assertTrue(bDefaultUnit);
        Assert.assertEquals(2, listOfArrayData.size());
        Assert.assertArrayEquals(arr1, (Double[]) listOfArrayData.get(0).values());
        Assert.assertArrayEquals(arr2, (Double[]) listOfArrayData.get(1).values());
        Assert.assertArrayEquals(arr2, arrayOfDoubles);
        Assert.assertArrayEquals(arr1, head.toArray());
    }

    @Test
    public void showUsageOfMatrices() {
        //#matrices
        //make some arrays
        Byte[][] m1 = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        Byte[][] m2 = {{1, 2, 3, 4, 5}, {10, 20, 30, 40, 50}};

        //keys
        Key<MatrixData<Byte>> encoderKey = JKeyType.ByteMatrixKey().make("encoder", JUnits.NoUnits);

        //Store some values using helper method in ArrayData
        Parameter<MatrixData<Byte>> p1 = encoderKey.set(
                MatrixData.fromArrays(m1),
                MatrixData.fromArrays(m2));
        Parameter<MatrixData<Byte>> p2 = encoderKey.set(
                MatrixData.fromArrays(m2)
        ).withUnits(JUnits.liter);

        //add units to existing parameters
        Parameter<MatrixData<Byte>> p1AsLiter = p1.withUnits(JUnits.liter);

        //default unit is NoUnits()
        boolean bDefaultUnit = JUnits.NoUnits == p1.units();

        //retrieving values
        MatrixData<Byte> head = p1.head();
        List<MatrixData<Byte>> matrixData1 = p1.jValues();
        List<MatrixData<Byte>> matrixData2 = p2.jValues();

        //#matrices

        //validations
        Assert.assertEquals(JUnits.NoUnits, p1.units());
        Assert.assertEquals(JUnits.liter, p2.units());
        Assert.assertEquals(JUnits.liter, p1AsLiter.units());
        Assert.assertTrue(bDefaultUnit);
        Assert.assertEquals(2, matrixData1.size());
        Assert.assertEquals(5, head.apply(1,1).intValue());
        Assert.assertEquals(Arrays.asList(m1[0]), head.jValues().get(0));
        Assert.assertArrayEquals(m1, (Byte[][]) matrixData1.get(0).values());
        Assert.assertArrayEquals(m2, (Byte[][]) matrixData2.get(0).values());
        Assert.assertArrayEquals(m1, head.values());
    }

    @Test
    public void showUsageOfChoice() {
        //#choice
        //Choice
        final Choices choices = Choices.from("A", "B", "C");

        //keys
        GChoiceKey choice1Key = JKeyType.ChoiceKey().make("mode", JUnits.NoUnits, choices);
        GChoiceKey choice2Key = JKeyType.ChoiceKey().make(
                "mode-reset", JUnits.NoUnits,
                Choices.fromChoices(
                        new Choice("c"),
                        new Choice("b"),
                        new Choice("a")));

        //store values
        Parameter<Choice> p1 = choice1Key.set(new Choice("A")).withUnits(JUnits.foot);
        Parameter<Choice> p2 = choice2Key.set(new Choice("c"));

        //add units
        Parameter<Choice> paramWithFoot = p1.withUnits(JUnits.foot);

        //default unit is NoUnits()
        boolean bDefaultUnit = JUnits.NoUnits == p2.units();

        //retrieving values
        Choice head = p1.head();
        List<Choice> values = p2.jValues();
        //#choice

        //validations
        Assert.assertEquals(JUnits.foot, p1.units());
        Assert.assertEquals(JUnits.NoUnits, p2.units());
        Assert.assertEquals(JUnits.foot, paramWithFoot.units());
        Assert.assertTrue(bDefaultUnit);
        Assert.assertEquals("A", head.name());
        Assert.assertEquals(values, Arrays.asList(new Choice("c")));
    }

    @Test
    public void showUsageOfRaDec() {
        //#radec
        //RaDec
        RaDec raDec1 = new RaDec(1.0, 2.0);
        RaDec raDec2 = new RaDec(3.0, 4.0);

        //keys
        Key<RaDec> raDecKey = JKeyType.RaDecKey().make("raDecKey", JUnits.NoUnits);

        //store values
        Parameter<RaDec> p1 = raDecKey.set(raDec1);
        Parameter<RaDec> p2 = raDecKey.set(raDec1, raDec2).withUnits(JUnits.degree);

        //add units
        Parameter<RaDec> paramWithDegree = p1.withUnits(JUnits.degree);

        //default unit is NoUnits()
        boolean bDefaultUnit = JUnits.NoUnits == p1.units();

        //retrieving values
        RaDec head = p1.head();
        List<RaDec> values = p2.jValues();
        //#radec

        //validations
        Assert.assertEquals(JUnits.NoUnits, p1.units());
        Assert.assertEquals(JUnits.degree, p2.units());
        Assert.assertEquals(JUnits.degree, paramWithDegree.units());
        Assert.assertTrue(bDefaultUnit);
        Assert.assertEquals(raDec1, head);
        Assert.assertEquals(values, Arrays.asList(raDec1, raDec2));
    }

    @Test
    public void showUsageOfCoords() {
        //#coords

        //import csw.params.core.models.Coords.*;
        //import static csw.params.core.models.JCoords.*;
        //import static csw.params.core.models.Coords.*;

        // Coordinate types
        ProperMotion pm = new ProperMotion(0.5, 2.33);

        EqCoord eqCoord = new EqCoord("12:13:14.15", "-30:31:32.3", FK5(), BASE(),
                DEFAULT_CATNAME(), pm.pmx(), pm.pmy());

        SolarSystemCoord solarSystemCoord = new SolarSystemCoord(BASE(), Venus());

        MinorPlanetCoord minorPlanetCoord = new MinorPlanetCoord(GUIDER1(), 2000, JAngle.degree(90),
                JAngle.degree(2), JAngle.degree(100), 1.4, 0.234, JAngle.degree(220));

        CometCoord cometCoord = new CometCoord(BASE(), 2000.0, JAngle.degree(90),
                JAngle.degree(2), JAngle.degree(100), 1.4, 0.234);

        AltAzCoord altAzCoord = new AltAzCoord(BASE(), JAngle.degree(301), JAngle.degree(42.5));

        // Can use base trait CoordKey to store values for all types
        Key<Coord> basePosKey = JKeyType.CoordKey().make("BasePosition", JUnits.NoUnits);

        Parameter<Coord> posParam = basePosKey.set(eqCoord, solarSystemCoord, minorPlanetCoord, cometCoord, altAzCoord);

        //retrieving values
        assert (posParam.jValues().size() == 5);
        assert (posParam.jValues().get(0).equals(eqCoord));
        assert (posParam.jValues().get(1).equals(solarSystemCoord));
        assert (posParam.jValues().get(2).equals(minorPlanetCoord));
        assert (posParam.jValues().get(3).equals(cometCoord));
        assert (posParam.jValues().get(4).equals(altAzCoord));
        //#coords
    }

    @Test
    public void showUsageOfStruct() {
        //#struct
        //keys
        Key<Struct> skey = JKeyType.StructKey().make("myStruct", JUnits.NoUnits);

        Key<String> ra = JKeyType.StringKey().make("ra", JUnits.NoUnits);
        Key<String> dec = JKeyType.StringKey().make("dec", JUnits.NoUnits);
        Key<Double> epoch = JKeyType.DoubleKey().make("epoch", JUnits.year);

        //initialize struct
        Struct struct1 = new Struct().madd(
                ra.set("12:13:14.1"),
                dec.set("32:33:34.4"),
                epoch.set(1950.0));
        Struct struct2 = new Struct().madd(
                dec.set("32:33:34.4"),
                ra.set("12:13:14.1"),
                epoch.set(1970.0));

        //make parameters
        Parameter<Struct> p1 = skey.set(struct1);
        Parameter<Struct> p2 = skey.set(struct1, struct2);

        //add units
        Parameter<Struct> paramWithLightYear = p1.withUnits(JUnits.lightyear);

        //default unit is NoUnits()
        boolean bDefaultUnit = JUnits.NoUnits == p1.units();

        //retrieving values
        Struct head = p1.head();
        List<Struct> structs = p2.jValues();

        //get individual keys
        Optional<Parameter<String>> firstKey = struct1.jGet(JKeyType.StringKey().make("ra", JUnits.NoUnits));
        Optional<Parameter<String>> secondKey = struct1.jGet("dec", JKeyType.StringKey());
        Optional<Parameter<Double>> thirdKey = struct1.jGet("epoch", JKeyType.DoubleKey());

        //access parameter using 'parameter' and 'apply' method
        boolean bSuccess = struct1.parameter(ra) == struct1.apply(ra);

        //remove a parameter and verify it doesn't exist
        Struct mutated1 = struct1.remove(ra); //using key
        Struct mutated2 = struct1.remove(firstKey.orElseThrow());

        //find out missing keys
        Set<String> missingKeySet = mutated1.jMissingKeys(ra, dec, epoch, JKeyType.StringKey().make("someRandomKey", JUnits.NoUnits));
        Set<String> expectedMissingKeys = Set.of("ra", "someRandomKey");
        //#struct

        //validations
        Assert.assertTrue(bDefaultUnit);
        Assert.assertEquals(struct1, head);
        Assert.assertEquals(JUnits.lightyear, paramWithLightYear.units());
        Assert.assertEquals(struct1.parameter(dec), secondKey.orElseThrow());
        Assert.assertEquals(struct1.parameter(epoch), thirdKey.orElseThrow());
        Assert.assertEquals(expectedMissingKeys, missingKeySet);
        Assert.assertFalse(mutated1.exists(ra));
        Assert.assertFalse(mutated2.exists(ra));
    }

    @Test
    public void showUsageOfUnits() {
        //#units
        //declare keyname
        String s1 = "encoder";

        //making 3 keys
        Key<Boolean> k1 = JKeyType.BooleanKey().make(s1);
        Key<Short> k2 = JKeyType.ShortKey().make("RandomKeyName", JUnits.NoUnits);
        Key<String> k3 = JKeyType.StringKey().make(s1, JUnits.NoUnits);

        //storing a single value, default unit is NoUnits()
        Parameter<Boolean> bParam = k1.set(true);
        boolean bDefaultUnitSet = bParam.units() == JUnits.NoUnits; //true

        //default unit for UTCTimeKey
        Parameter<UTCTime> utcParam = JKeyType
                .UTCTimeKey()
                .make("now")
                .set(UTCTime.now());

        //default unit for TAITimeKey
        Parameter<TAITime> taiParam = JKeyType
                .TAITimeKey()
                .make("now")
                .set(TAITime.now());


        //storing multiple values
        Parameter<Short> paramOfShorts = k2.set(
                (short) 1,
                (short) 2,
                (short) 3,
                (short) 4
        );

        //values to store
        String[] weekDays = {"Sunday", "Monday", "Tuesday"};

        //defaults units via set
        Parameter<String> paramWithUnits1 = k3.setAll(weekDays);
        //associating units via withUnits
        Parameter<String> paramWithUnits2 = k3.setAll(weekDays).withUnits(JUnits.day);
        //change existing unit
        Parameter<Short> paramWithUnits3 = paramOfShorts.withUnits(JUnits.meter);
        //#units

        //validations
        Assert.assertTrue(bDefaultUnitSet);
        Assert.assertSame(utcParam.units(), JUnits.utc);
        Assert.assertSame(taiParam.units(), JUnits.tai);
        Assert.assertSame(paramWithUnits1.units(), JUnits.NoUnits);
        Assert.assertSame(paramWithUnits2.units(), JUnits.day);
        Assert.assertSame(paramWithUnits3.units(), JUnits.meter);
    }
}
