/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package org.tmt.csw.sample;

import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.JComponentType;
import csw.location.api.models.PekkoLocation;
import csw.location.api.models.ComponentId;
import csw.location.api.models.Connection.PekkoConnection;
import csw.prefix.javadsl.JSubsystem;
import csw.prefix.models.Prefix;
import csw.testkit.javadsl.FrameworkTestKitJunitResource;
import csw.testkit.javadsl.JCSWService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

// DEOPSCSW-39: examples of Location Service
//#intro
public class JSampleTest {
    @ClassRule
    public static final FrameworkTestKitJunitResource testKit =
            new FrameworkTestKitJunitResource(Arrays.asList(JCSWService.AlarmServer, JCSWService.EventServer));
//#intro

    //#setup
    @BeforeClass
    public static void setup() {
        testKit.spawnStandalone(com.typesafe.config.ConfigFactory.load("JSampleStandalone.conf"));
    }
    //#setup

    //#locate
    @Test
    public void testAssemblyShouldBeLocatableUsingLocationService() throws ExecutionException, InterruptedException {
        PekkoConnection connection = new PekkoConnection(new ComponentId(Prefix.apply(JSubsystem.CSW, "sample"), JComponentType.Assembly));
        ILocationService locationService = testKit.jLocationService();
        PekkoLocation location = locationService.resolve(connection, Duration.ofSeconds(10)).get().orElseThrow();

        Assert.assertEquals(location.connection(), connection);
    }
    //#locate
}
