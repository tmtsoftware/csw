/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.core;

import csw.time.core.models.UTCTime;
import csw.time.core.models.utils.JTestProperties;
import csw.time.core.models.utils.TestProperties;
import csw.time.core.models.utils.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static csw.time.core.utils.Eventually.eventually;
import static org.junit.Assert.assertFalse;

public class JTMTTimeHelperTest {
    private final TestProperties testProperties = JTestProperties.instance();

    //DEOPSCSW-541: PTP accuracy and precision while reading remote location time
    //DEOPSCSW-549: Time service api
    @Test
    public void should_get_maximum_precision_while_reading_remote_location_utc_time__DEOPSCSW_541_DEOPSCSW_549() {
        eventually(java.time.Duration.ofSeconds(5), () -> {
            UTCTime utcTime = UTCTime.now();

            ZonedDateTime utcHawaiiTime = TMTTimeHelper.atHawaii(utcTime);
            ZonedDateTime utcLocalTime = TMTTimeHelper.atLocal(utcTime);

            assertFalse(TestUtil.formatWithPrecision(utcTime.value(), testProperties.precision()).endsWith("000"));
            assertFalse(TestUtil.formatWithPrecision(utcHawaiiTime.toInstant(), testProperties.precision()).endsWith("000"));
            assertFalse(TestUtil.formatWithPrecision(utcLocalTime.toInstant(), testProperties.precision()).endsWith("000"));
        });

    }

    //DEOPSCSW-539: Ability to read local time that is synchronized with PTP time, at remote observing sites
    //DEOPSCSW-549: Time service api
    @Test
    public void should_get_hawaii_local_date_time_and_date_time_at_provided_zone_id_from_utctime__DEOPSCSW_539_DEOPSCSW_549() {
        ZoneId hawaiiZone = ZoneId.of("US/Hawaii");

        Instant instant = ZonedDateTime.of(
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
        ZonedDateTime localZDT = instant.atZone(ZoneId.systemDefault());

        // Using TimeService you can get this utcInstant, which is synchronized with PTP
        // and then you can use atHawaii, atLocal and atZone helpers defined on CswInstant
        UTCTime utcTime = new UTCTime(instant);

        Assert.assertEquals(localZDT, TMTTimeHelper.atLocal(utcTime));
        Assert.assertEquals(hawaiiZDT, TMTTimeHelper.atHawaii(utcTime));
    }
}
