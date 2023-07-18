/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.database;

import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.SpawnProtocol;
import csw.database.commons.DBTestHelper;
import csw.database.exceptions.DatabaseException;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;

//DEOPSCSW-615: DB service accessible to CSW component developers
public class JDatabaseServiceFactoryFailureTest {

    private static ActorSystem<SpawnProtocol.Command> system;
    private static EmbeddedPostgres postgres;
    private static DatabaseServiceFactory dbFactory;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.apply(SpawnProtocol.create(), "test");
        dbFactory = DBTestHelper.dbServiceFactory(system);
        postgres = DBTestHelper.postgres(0); // 0 is random port
    }

    @AfterClass
    public static void afterAll() throws Exception {
        postgres.close();
        system.terminate();
        Await.result(system.whenTerminated(), Duration.apply(5, SECONDS));
    }

    @Test
    public void shouldThrowDatabaseConnectionWhileConnectingWithIncorrectPort__DEOPSCSW_615() {
        ExecutionException ex = Assert.assertThrows(ExecutionException.class, () -> dbFactory.jMakeDsl().get());
        Assert.assertTrue(ex.getCause() instanceof DatabaseException);
    }
}
