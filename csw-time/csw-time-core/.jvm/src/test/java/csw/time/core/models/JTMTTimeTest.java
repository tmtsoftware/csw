package csw.time.core.models;

import csw.time.core.models.utils.JTestProperties;
import csw.time.core.models.utils.TestProperties;
import csw.time.core.models.utils.TestUtil;
import csw.time.clock.natives.models.TMTClock$;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JTMTTimeTest extends JUnitSuite {

    private static int TaiOffset = 37;
    private TestProperties testProperties = JTestProperties.instance();

    @BeforeClass
    public static void setup() {
        TMTClock$.MODULE$.clock().setTaiOffset(TaiOffset);
    }

    //------------------------------UTC-------------------------------

    //DEOPSCSW-532: Synchronize activities with other comp. using UTC
    // This test is sufficient to show code works in both Scala and Java
    // since UTCTime.now is used in both languages.
    @Test
    public void should_get_utc_time() {
        UTCTime utcTime = UTCTime.now();
        Instant fixedInstant = Instant.now();

        long expectedMillis = fixedInstant.toEpochMilli();

        assertEquals((double) expectedMillis, (double) utcTime.value().toEpochMilli(), 5);
    }

    //DEOPSCSW-537: Scala and Java API for conversion between TAI and UTC
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_convert_utc_to_tai() {
        UTCTime utcTime = UTCTime.now();
        TAITime taiTime = utcTime.toTAI();

        assertEquals(TaiOffset, Duration.between(utcTime.value(), taiTime.value()).getSeconds());
    }

    //DEOPSCSW-534: PTP accuracy and precision while reading UTC
    @Test
    public void should_get_maximum_precision_supported_by_system_in_utc() {
        assertFalse(TestUtil.formatWithPrecision(UTCTime.now().value(), testProperties.precision()).endsWith("000"));
    }

    //------------------------------TAI-------------------------------

    //DEOPSCSW-535: Synchronize activities with other comp, using TAI
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_get_tai_time() {
        TAITime taiTime = TAITime.now();
        Instant expectedTaiInstant = Instant.now().plusSeconds(TaiOffset);

        long expectedMillis = expectedTaiInstant.toEpochMilli();

        assertEquals(expectedMillis, taiTime.value().toEpochMilli(), 5);
    }

    @Test
    public void should_get_tai_offset() {
        assertEquals(TaiOffset, TAITime.offset());
    }

    //DEOPSCSW-537: Scala and Java API for conversion between TAI and UTC
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_convert_tai_to_utc() {
        TAITime taiTime = TAITime.now();
        UTCTime utcTime = taiTime.toUTC();

        assertEquals(TaiOffset, Duration.between(utcTime.value(), taiTime.value()).getSeconds());
    }

    //DEOPSCSW-538: PTP accuracy and precision while reading TAI
    @Test
    public void should_get_maximum_precision_supported_by_system_in_tai() {
        assertFalse(TestUtil.formatWithPrecision(TAITime.now().value(), testProperties.precision()).endsWith("000"));
    }
}
