package csw.params.core.models;

import org.junit.Test;
import org.scalatestplus.junit.JUnitSuite;
import scala.Tuple2;

import static org.junit.Assert.*;
import static csw.params.core.models.JAngle.*;

// Tests the Angle class usage from Java (including helper methods in JAngle)
public class JAngleTest extends JUnitSuite {
    private double delta = 0.00000001;

    // Basic parsing of radec as strings

    @Test
    public void shouldAllowBasicParsingUsingAngle() {
        assertEquals(
            new Tuple2<>(Angle.parseRa("20 54 05.689"), Angle.parseDe("+37 01 17.38")),
            Angle.parseRaDe("20 54 05.689 +37 01 17.38"));

        assertEquals(
            new Tuple2<>(Angle.parseRa("10:12:45.3"), Angle.parseDe("-45:17:50")),
            Angle.parseRaDe("10:12:45.3-45:17:50"));

        assertEquals(
            new Tuple2<>(Angle.parseRa("15h17m"), Angle.parseDe("-11d10m")),
            Angle.parseRaDe("15h17m-11d10m"));

        assertEquals(
            new Tuple2<>(Angle.parseRa("275d11m15.6954s"), Angle.parseDe("+17d59m59.876s")),
            Angle.parseRaDe("275d11m15.6954s+17d59m59.876s"));
    }

    @Test
    public void shouldAllowUsingImplicits() {
        // Test using similar functionality in Java
        assertEquals(
            new Tuple2<>(arcHour(12.34567), degree(-17.87654d)),
            Angle.parseRaDe("12.34567h-17.87654d"));

        assertEquals(
            new Tuple2<>(arcHour(12.34567), degree(-17.87654d)),
            Angle.parseRaDe("12.34567h-17.87654d"));

        assertEquals(
            new Tuple2<>(degree(350.123456), degree(-17.33333)),
            Angle.parseRaDe("350.123456d-17.33333d"));

        assertEquals(
            new Tuple2<>(degree(350.123456), degree(-17.33333)),
            Angle.parseRaDe("350.123456 -17.33333"));
    }

    // "Test parsing"
    @SuppressWarnings("PointlessArithmeticExpression")
    @Test
    public void shouldAllowTestingOfParserToMicroarcsecs1() {
        assertEquals(
            Angle.parseRa("1", "2", "3").uas(),
            1L * 15L * 60L * 60L * 1000L * 1000L + 2L * 15L * 60L * 1000L * 1000L + 3L * 15L * 1000L * 1000L);

        assertEquals(
            Angle
                .parseDe("+", "1", "2", "3").uas(),
            1L * 60L * 60L * 1000L * 1000L + 2L * 60L * 1000L * 1000L + 3L * 1000L * 1000L);
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    @Test
    public void shouldAllowTestingOfParserToMicroarcsecs2() {
        assertEquals(Angle.parseRa("1h2m3s").uas(),
            1L * 15L * 60L * 60L * 1000L * 1000L + 2L * 15L * 60L * 1000L * 1000L + 3L * 15L * 1000L * 1000L);

        assertEquals(Angle.parseRa("02 51.2").uas(),
            2L * 15L * 60L * 60L * 1000L * 1000L + 512L * 15L * 60L * 1000L * 100L);

        assertEquals(Angle.parseDe("+1d2'3\"").uas(),
            1L * 60L * 60L * 1000L * 1000L + 2L * 60L * 1000L * 1000L + 3L * 1000L * 1000L);

        assertEquals(Angle.parseDe("-1d2'3\"").uas(),
            -(1L * 60L * 60L * 1000L * 1000L + 2L * 60L * 1000L * 1000L + 3L * 1000L * 1000L));

        assertEquals(Angle.parseDe("+13 12").uas(),
            13L * 60L * 60L * 1000L * 1000L + 12L * 60L * 1000L * 1000L);
    }


    // conversion tests
    @Test
    public void shouldAllowConversions() {
        assertEquals(Angle.D2R() * 1d, Math.toRadians(1d), delta);
        assertEquals(Angle.R2D() * 1d, Math.toDegrees(1d), delta);
        assertEquals(Angle.H2D() * 1d, 15d, delta);
        assertEquals(Angle.D2H() * 1d, 1d / 15d, delta);
        assertEquals(Angle.D2M(), 60d, delta);
        assertEquals(Angle.M2D(), 1d / 60d, delta);
        assertEquals(Angle.D2S(), 3600d, delta);
        assertEquals(Angle.S2D(), 1d / 3600d, delta);
        assertEquals(Angle.H2R() * 1d, Math.toRadians(15d), delta);
        assertEquals(Angle.R2H() * Math.toRadians(15d), 1d, delta);
        assertEquals(Angle.M2R() * 60d, Math.toRadians(1d), delta);
        assertEquals(Angle.R2M() * Math.toRadians(1d), 60d, delta);
        assertEquals(Angle.Mas2R(), Angle.D2R() / 3600000d, delta);
        assertEquals(Angle.R2Mas(), 1d / Angle.Mas2R(), delta);
    }

    // Should allow distance calculation
    @Test
    public void shouldDoDistance() {
        assertEquals(Angle.distance(Angle.D2R() * 1d, 0d, Angle.D2R() * 2d, 0d), Angle.D2R() * 1d, delta);
        assertEquals(Angle.distance(0, Angle.D2R() * 90d, Angle.D2R() * 180d, -(Angle.D2R() * 90d)), Angle.D2R() * 180d, delta);
    }

    // Positions to String
    @Test
    public void shouldConvertRaToString() {
        assertEquals("11h", Angle.raToString(Angle.H2R() * 11));
        assertEquals("11h 12m", Angle.raToString(Angle.H2R() * 11 + Angle.H2R() * 12 / 60));
        assertEquals("11h 12m 13s", Angle.raToString(Angle.H2R() * 11 + Angle.H2R() * 12 / 60 + Angle.H2R() * 13 / 3600));
        assertEquals("11h 12m 13.3s", Angle.raToString(Angle.H2R() * 11 + Angle.H2R() * 12 / 60 + Angle.H2R() * 13.3 / 3600));
    }

    @Test
    public void shouldConvertDecToString() {
        assertEquals("11" + Angle.DEGREE_SIGN(), Angle.deToString(Angle.D2R() * 11));
        assertEquals("11" + Angle.DEGREE_SIGN() + "12'", Angle.deToString(Angle.D2R() * 11 + Angle.M2R() * 12));
        assertEquals("11" + Angle.DEGREE_SIGN() + "12'13\"", Angle.deToString(Angle.D2R() * 11 + Angle.M2R() * 12 + Angle.S2R() * 13));
        assertEquals("11" + Angle.DEGREE_SIGN() + "12'13.3\"", Angle.deToString(Angle.D2R() * 11 + Angle.M2R() * 12 + Angle.S2R() * 13.3));
        assertEquals("-11" + Angle.DEGREE_SIGN() + "12'", Angle.deToString(-(Angle.D2R() * 11 + Angle.M2R() * 12)));
    }
}
