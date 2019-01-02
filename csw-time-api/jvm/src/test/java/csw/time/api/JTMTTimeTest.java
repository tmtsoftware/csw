package csw.time.api;

import csw.time.api.utils.JTestProperties;
import csw.time.api.utils.TestProperties;
import csw.time.api.utils.TestUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.time.*;

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

    // DEOPSCSW-539: Ability to read local time that is synchronized with PTP time, at remote observing sites
    @Test
    public void should_get_hawaii_local_date_time_and_date_time_at_provided_zone_id_from_utctime() {
        ZoneId hawaiiZone = ZoneId.of("US/Hawaii");

        Instant instant   = ZonedDateTime.of(
                2007,
                12,
                3,
                10,
                15,
                30,
                11,
                ZoneOffset.UTC
        ).toInstant();

        ZonedDateTime hawaiiZDT = instant.atZone(hawaiiZone);
        ZonedDateTime localZDT  = instant.atZone(ZoneId.systemDefault());

        // Using TimeService you can get this utcInstant, which is synchronized with PTP
        // and then you can use atHawaii, atLocal and atZone helpers defined on CswInstant
        UTCTime utcTime = new UTCTime(instant);

        Assert.assertEquals(localZDT, utcTime.atLocal());
        Assert.assertEquals(hawaiiZDT, utcTime.atHawaii());
    }
}
