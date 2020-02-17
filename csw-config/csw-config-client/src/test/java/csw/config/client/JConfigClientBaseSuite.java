package csw.config.client;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import akka.actor.typed.javadsl.Behaviors;
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

    private csw.location.server.internal.ServerWiring locationWiring = new csw.location.server.internal.ServerWiring(false);

    private ActorRuntime actorRuntime = new ActorRuntime(ActorSystem.create(SpawnProtocol.create(), "Guardian"));
    private ILocationService clientLocationService = JHttpLocationServiceFactory.makeLocalClient(actorRuntime.typedSystem());

    public IConfigService configService = JConfigClientFactory.adminApi(actorRuntime.typedSystem(), clientLocationService, factory());
    public IConfigClientService configClientApi = JConfigClientFactory.clientApi(actorRuntime.typedSystem(), clientLocationService);
    public ActorSystem<SpawnProtocol.Command> system = (ActorSystem<SpawnProtocol.Command>) actorRuntime.typedSystem();

    private ServerWiring serverWiring = ServerWiring$.MODULE$.make(securityDirectives());
    private HttpService httpService = serverWiring.httpService();
    private TestFileUtils testFileUtils = new TestFileUtils(serverWiring.settings());
    private FiniteDuration timeout = Duration.create(10, "seconds");

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
        actorRuntime.typedSystem().terminate();
        Await.result(actorRuntime.typedSystem().whenTerminated(), timeout);
        Await.result(serverWiring.actorRuntime().classicSystem().terminate(), timeout);
        Await.result(locationWiring.actorRuntime().shutdown(), timeout);
    }
}
