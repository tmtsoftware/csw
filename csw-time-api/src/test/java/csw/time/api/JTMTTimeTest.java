package csw.time.api;

import csw.time.api.utils.JTestProperties;
import csw.time.api.utils.TestProperties;
import csw.time.api.utils.TestUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JTMTTimeTest extends JUnitSuite {

    private static int TaiOffset = 37;
    private TestProperties testProperties = JTestProperties.instance();

    @BeforeClass
    public static void setup(){
        TAITime.setOffset(TaiOffset);
    }

    //------------------------------UTC-------------------------------

    //DEOPSCSW-533: Access parts of UTC date.time in Java and Scala
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

    //DEOPSCSW-541: PTP accuracy and precision while reading remote location time
    @Test
    public void should_get_maximum_precision_while_reading_remote_location_utc_time() {
        UTCTime utcTime = UTCTime.now();

        ZonedDateTime utcHawaiiTime = utcTime.atHawaii();
        ZonedDateTime utcLocalTime = utcTime.atLocal();

        assertFalse(TestUtil.formatWithPrecision(utcTime.value(), testProperties.precision()).endsWith("000"));
        assertFalse(TestUtil.formatWithPrecision(utcHawaiiTime.toInstant(), testProperties.precision()).endsWith("000"));
        assertFalse(TestUtil.formatWithPrecision(utcLocalTime.toInstant(), testProperties.precision()).endsWith("000"));
    }

    //------------------------------TAI-------------------------------

    //DEOPSCSW-536: Access parts of TAI date/time in Java and Scala
    //DEOPSCSW-530: SPIKE: Get TAI offset and convert to UTC and Vice Versa
    @Test
    public void should_get_tai_time() {
        TAITime taiTime = TAITime.now();
        Instant expectedTaiInstant = Instant.now().plusSeconds(TaiOffset);

        long expectedMillis = expectedTaiInstant.toEpochMilli();

        assertEquals(expectedMillis, taiTime.value().toEpochMilli(), 5);
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

    //DEOPSCSW-541: PTP accuracy and precision while reading remote location time
    @Test
    public void should_get_maximum_precision_while_reading_remote_location_tai_time() {
        TAITime taiTime = TAITime.now();

        assertFalse(TestUtil.formatWithPrecision(taiTime.value(), testProperties.precision()).endsWith("000"));
    }

}
