package csw.services.location.javadsl;

import akka.Done;
import csw.services.location.models.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ILocationService {
    CompletableFuture<RegistrationResult> register(Registration registration);

    CompletableFuture<List<Location>> list();

    CompletableFuture<Done> unregisterAll();

    CompletableFuture<Resolved> resolve(Connection connection);
}
