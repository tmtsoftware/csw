package csw.config.client;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import csw.config.api.javadsl.IConfigClientService;
import csw.config.api.javadsl.IConfigService;
import csw.config.client.internal.ActorRuntime;
import csw.config.client.javadsl.JConfigClientFactory;
import csw.config.server.ServerWiring;
import csw.config.server.ServerWiring$;
import csw.config.server.commons.TestFileUtils;
import csw.config.server.http.HttpService;
import csw.config.server.mocks.JMockedAuthentication;
import csw.location.api.javadsl.ILocationService;
import csw.location.client.javadsl.JHttpLocationServiceFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class JConfigClientBaseSuite extends JMockedAuthentication {

    private final csw.location.server.internal.ServerWiring locationWiring = new csw.location.server.internal.ServerWiring(false);

    private final ActorRuntime actorRuntime = new ActorRuntime(ActorSystem.create(SpawnProtocol.create(), "Guardian"));
    private final ILocationService clientLocationService = JHttpLocationServiceFactory.makeLocalClient(actorRuntime.actorSystem());

    public IConfigService configService = JConfigClientFactory.adminApi(actorRuntime.actorSystem(), clientLocationService, factory());
    public IConfigClientService configClientApi = JConfigClientFactory.clientApi(actorRuntime.actorSystem(), clientLocationService);
    public ActorSystem<SpawnProtocol.Command> system = (ActorSystem<SpawnProtocol.Command>) actorRuntime.actorSystem();

    private final ServerWiring serverWiring = ServerWiring$.MODULE$.make(securityDirectives());
    private final HttpService httpService = serverWiring.httpService();
    private final TestFileUtils testFileUtils = new TestFileUtils(serverWiring.settings());
    private final FiniteDuration timeout = Duration.create(10, "seconds");

    public void setup() throws Exception {
        Await.result(locationWiring.locationHttpService().start("127.0.0.1"), timeout);
        Await.result(httpService.registeredLazyBinding(), timeout);
    }

    public void initSvnRepo() {
        serverWiring.svnRepo().initSvnRepo();
    }

    public void deleteServerFiles() {
        testFileUtils.deleteServerFiles();
    }

    public void cleanup() throws Exception {
        Await.result(httpService.shutdown(), timeout);
        actorRuntime.actorSystem().terminate();
        Await.result(actorRuntime.actorSystem().whenTerminated(), timeout);
        Await.result(serverWiring.actorRuntime().classicSystem().terminate(), timeout);
        Await.result(locationWiring.actorRuntime().shutdown(), timeout);
    }
}
