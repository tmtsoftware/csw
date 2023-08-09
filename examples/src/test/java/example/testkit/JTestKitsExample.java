/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.testkit;

import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.SpawnProtocol;
import com.typesafe.config.ConfigFactory;
import csw.location.api.javadsl.ILocationService;
import csw.location.api.javadsl.JComponentType;
import csw.location.api.models.PekkoLocation;
import csw.location.api.models.ComponentId;
import csw.location.api.models.Connection.*;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import csw.prefix.models.Prefix;
import csw.prefix.javadsl.JSubsystem;
import csw.testkit.FrameworkTestKit;
import csw.testkit.javadsl.JCSWService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class JTestKitsExample {

    //#framework-testkit
    private static final FrameworkTestKit frameworkTestKit = FrameworkTestKit.create();

    @BeforeClass
    public static void beforeAll() {
        frameworkTestKit.start(JCSWService.ConfigServer, JCSWService.EventServer);
    }

    @AfterClass
    public static void afterAll() {
        frameworkTestKit.shutdown();
    }
    //#framework-testkit

    private final ActorSystem<SpawnProtocol.Command> system = frameworkTestKit.actorSystem();
    private final ILocationService locationService =
            JHttpLocationServiceFactory.makeLocalClient(system);

    @Test
    public void shouldAbleToSpawnContainerUsingTestKit() throws ExecutionException, InterruptedException {
        //#spawn-using-testkit

        // starting container from container config using testkit
        frameworkTestKit.spawnContainer(ConfigFactory.load("JSampleContainer.conf"));

        // starting standalone component from config using testkit
        // ActorRef<ComponentMessage> componentRef =
        //      frameworkTestKit.spawnStandaloneComponent(ConfigFactory.load("SampleHcdStandalone.conf"));

        //#spawn-using-testkit

        PekkoConnection connection = new PekkoConnection(new ComponentId(Prefix.apply(JSubsystem.CSW, "sample"), JComponentType.Assembly));
        Optional<PekkoLocation> pekkoLocation = locationService.resolve(connection, Duration.ofSeconds(5)).get();

        Assert.assertTrue(pekkoLocation.isPresent());
        Assert.assertEquals(connection, pekkoLocation.orElseThrow().connection());
    }

}
