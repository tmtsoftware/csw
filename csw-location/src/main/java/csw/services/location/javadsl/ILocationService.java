package csw.services.location.javadsl;

import akka.Done;
import akka.stream.KillSwitch;
import csw.services.location.models.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import akka.stream.javadsl.Source;

public interface ILocationService {
    CompletionStage<RegistrationResult> register(Registration registration);

    CompletionStage<Done> unregister(Connection connection);

    CompletionStage<Done> unregisterAll();

    CompletionStage<Resolved> resolve(Connection connection);

    CompletionStage<Set<Resolved>> resolve(Set<Connection> connections);

    CompletionStage<List<Location>> list();

    CompletionStage<List<Location>> list(ComponentType componentType);

    CompletionStage<List<Resolved>> list(String hostname);

    CompletionStage<List<Location>> list(ConnectionType connectionType);

    Source<Location, KillSwitch> track(Connection connection);
}
