package csw.time.client;

import csw.time.api.models.UTCTime;
import csw.time.client.utils.JTestProperties;
import csw.time.client.utils.TestProperties;
import csw.time.client.utils.TestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.Assert.assertFalse;

public class JTMTTimeHelperTest extends JUnitSuite {
    private TestProperties testProperties = JTestProperties.instance();

    //DEOPSCSW-541: PTP accuracy and precision while reading remote location time
    @Test
    public void should_get_maximum_precision_while_reading_remote_location_utc_time() {
        UTCTime utcTime = UTCTime.now();

        ZonedDateTime utcHawaiiTime = TMTTimeHelper.atHawaii(utcTime);
        ZonedDateTime utcLocalTime = TMTTimeHelper.atLocal(utcTime);

        assertFalse(TestUtil.formatWithPrecision(utcTime.value(), testProperties.precision()).endsWith("000"));
        assertFalse(TestUtil.formatWithPrecision(utcHawaiiTime.toInstant(), testProperties.precision()).endsWith("000"));
        assertFalse(TestUtil.formatWithPrecision(utcLocalTime.toInstant(), testProperties.precision()).endsWith("000"));
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

        Assert.assertEquals(localZDT, TMTTimeHelper.atLocal(utcTime));
        Assert.assertEquals(hawaiiZDT, TMTTimeHelper.atHawaii(utcTime));
    }
}
