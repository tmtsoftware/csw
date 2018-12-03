package csw.config.client;

import akka.actor.ActorSystem;
import akka.actor.CoordinatedShutdown;
import akka.http.javadsl.Http;
import akka.stream.Materializer;
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

import java.util.concurrent.TimeUnit;

public class JConfigClientBaseSuite extends JMockedAuthentication {

    private csw.location.server.internal.ServerWiring locationWiring = new csw.location.server.internal.ServerWiring();

    private ActorRuntime actorRuntime = new ActorRuntime(ActorSystem.create());
    private ILocationService clientLocationService = JHttpLocationServiceFactory.makeLocalClient(actorRuntime.actorSystem(), actorRuntime.mat());

    public IConfigService configService = JConfigClientFactory.adminApi(actorRuntime.actorSystem(), clientLocationService, factory());
    public IConfigClientService configClientApi = JConfigClientFactory.clientApi(actorRuntime.actorSystem(), clientLocationService);

    private ServerWiring serverWiring = ServerWiring$.MODULE$.make(securityDirectives());
    private HttpService httpService = serverWiring.httpService();
    private TestFileUtils testFileUtils = new TestFileUtils(serverWiring.settings());
    private FiniteDuration timeout = Duration.create(10, "seconds");

    public Materializer mat = actorRuntime.mat();

    public void setup() throws Exception {
        Await.result(locationWiring.locationHttpService().start(), timeout);
        Await.result(httpService.registeredLazyBinding(), timeout);
    }

    public void initSvnRepo() {
        serverWiring.svnRepo().initSvnRepo();
    }

    public void deleteServerFiles() {
        testFileUtils.deleteServerFiles();
    }

    public void cleanup() throws Exception {
        Http.get(actorRuntime.actorSystem()).shutdownAllConnectionPools().toCompletableFuture().get(10, TimeUnit.SECONDS);
        Await.result(httpService.shutdown(CoordinatedShutdown.unknownReason()), timeout);
        Await.result(actorRuntime.actorSystem().terminate(), timeout);
        Await.result(serverWiring.actorSystem().terminate(), timeout);
        Await.result(locationWiring.actorRuntime().shutdown(CoordinatedShutdown.unknownReason()), timeout);
    }
}
