package csw.services.location.javadsl;

import akka.Done;
import csw.services.location.models.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ILocationService {
        public CompletableFuture<RegistrationResult> register(Registration registration);

        public CompletableFuture<List<Location>> list();

        public CompletableFuture<Done> unregisterAll();

        public CompletableFuture<Resolved> resolve(Connection connection);
}
